/*
 * Copyright 2012-2022 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.boot.logging.logback;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.lang.reflect.Method;
import java.net.URL;
import java.security.CodeSource;
import java.security.ProtectionDomain;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.logging.ConsoleHandler;
import java.util.logging.Handler;
import java.util.logging.LogManager;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.jul.LevelChangePropagator;
import ch.qos.logback.classic.turbo.TurboFilter;
import ch.qos.logback.classic.util.ContextInitializer;
import ch.qos.logback.core.CoreConstants;
import ch.qos.logback.core.joran.spi.DefaultNestedComponentRegistry;
import ch.qos.logback.core.joran.spi.JoranException;
import ch.qos.logback.core.joran.util.beans.BeanDescription;
import ch.qos.logback.core.joran.util.beans.BeanDescriptionCache;
import ch.qos.logback.core.model.ComponentModel;
import ch.qos.logback.core.model.Model;
import ch.qos.logback.core.model.ModelUtil;
import ch.qos.logback.core.spi.FilterReply;
import ch.qos.logback.core.status.OnConsoleStatusListener;
import ch.qos.logback.core.status.Status;
import ch.qos.logback.core.util.StatusListenerConfigHelper;
import org.slf4j.ILoggerFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.Marker;
import org.slf4j.bridge.SLF4JBridgeHandler;

import org.springframework.aot.AotDetector;
import org.springframework.aot.generate.GenerationContext;
import org.springframework.aot.hint.MemberCategory;
import org.springframework.aot.hint.SerializationHints;
import org.springframework.aot.hint.TypeReference;
import org.springframework.beans.factory.aot.BeanFactoryInitializationAotContribution;
import org.springframework.beans.factory.aot.BeanFactoryInitializationAotProcessor;
import org.springframework.beans.factory.aot.BeanFactoryInitializationCode;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.boot.logging.AbstractLoggingSystem;
import org.springframework.boot.logging.LogFile;
import org.springframework.boot.logging.LogLevel;
import org.springframework.boot.logging.LoggerConfiguration;
import org.springframework.boot.logging.LoggingInitializationContext;
import org.springframework.boot.logging.LoggingSystem;
import org.springframework.boot.logging.LoggingSystemFactory;
import org.springframework.boot.logging.LoggingSystemProperties;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.Environment;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PropertiesLoaderUtils;
import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;
import org.springframework.util.ReflectionUtils;
import org.springframework.util.ResourceUtils;
import org.springframework.util.StringUtils;

/**
 * {@link LoggingSystem} for <a href="https://logback.qos.ch">logback</a>.
 *
 * @author Phillip Webb
 * @author Dave Syer
 * @author Andy Wilkinson
 * @author Ben Hale
 * @since 1.0.0
 */
public class LogbackLoggingSystem extends AbstractLoggingSystem implements BeanFactoryInitializationAotProcessor {

	private static final String BRIDGE_HANDLER = "org.slf4j.bridge.SLF4JBridgeHandler";

	private static final String CONFIGURATION_FILE_PROPERTY = "logback.configurationFile";

	private static final LogLevels<Level> LEVELS = new LogLevels<>();

	private LogbackModel logbackModel;

	static {
		LEVELS.map(LogLevel.TRACE, Level.TRACE);
		LEVELS.map(LogLevel.TRACE, Level.ALL);
		LEVELS.map(LogLevel.DEBUG, Level.DEBUG);
		LEVELS.map(LogLevel.INFO, Level.INFO);
		LEVELS.map(LogLevel.WARN, Level.WARN);
		LEVELS.map(LogLevel.ERROR, Level.ERROR);
		LEVELS.map(LogLevel.FATAL, Level.ERROR);
		LEVELS.map(LogLevel.OFF, Level.OFF);
	}

	private static final TurboFilter FILTER = new TurboFilter() {

		@Override
		public FilterReply decide(Marker marker, ch.qos.logback.classic.Logger logger, Level level, String format,
				Object[] params, Throwable t) {
			return FilterReply.DENY;
		}

	};

	public LogbackLoggingSystem(ClassLoader classLoader) {
		super(classLoader);
	}

	@Override
	public LoggingSystemProperties getSystemProperties(ConfigurableEnvironment environment) {
		return new LogbackLoggingSystemProperties(environment);
	}

	@Override
	protected String[] getStandardConfigLocations() {
		return new String[] { "logback-test.groovy", "logback-test.xml", "logback.groovy", "logback.xml" };
	}

	@Override
	public void beforeInitialize() {
		LoggerContext loggerContext = getLoggerContext();
		if (isAlreadyInitialized(loggerContext)) {
			return;
		}
		super.beforeInitialize();
		configureJdkLoggingBridgeHandler();
		loggerContext.getTurboFilterList().add(FILTER);
	}

	private void configureJdkLoggingBridgeHandler() {
		try {
			if (isBridgeJulIntoSlf4j()) {
				removeJdkLoggingBridgeHandler();
				SLF4JBridgeHandler.install();
			}
		}
		catch (Throwable ex) {
			// Ignore. No java.util.logging bridge is installed.
		}
	}

	private boolean isBridgeJulIntoSlf4j() {
		return isBridgeHandlerAvailable() && isJulUsingASingleConsoleHandlerAtMost();
	}

	private boolean isBridgeHandlerAvailable() {
		return ClassUtils.isPresent(BRIDGE_HANDLER, getClassLoader());
	}

	private boolean isJulUsingASingleConsoleHandlerAtMost() {
		java.util.logging.Logger rootLogger = LogManager.getLogManager().getLogger("");
		Handler[] handlers = rootLogger.getHandlers();
		return handlers.length == 0 || (handlers.length == 1 && handlers[0] instanceof ConsoleHandler);
	}

	private void removeJdkLoggingBridgeHandler() {
		try {
			removeDefaultRootHandler();
			SLF4JBridgeHandler.uninstall();
		}
		catch (Throwable ex) {
			// Ignore and continue
		}
	}

	private void removeDefaultRootHandler() {
		try {
			java.util.logging.Logger rootLogger = LogManager.getLogManager().getLogger("");
			Handler[] handlers = rootLogger.getHandlers();
			if (handlers.length == 1 && handlers[0] instanceof ConsoleHandler) {
				rootLogger.removeHandler(handlers[0]);
			}
		}
		catch (Throwable ex) {
			// Ignore and continue
		}
	}

	@Override
	public void initialize(LoggingInitializationContext initializationContext, String configLocation, LogFile logFile) {
		LoggerContext loggerContext = getLoggerContext();
		if (isAlreadyInitialized(loggerContext)) {
			return;
		}
		if (!AotDetector.useGeneratedArtifacts()
				|| !initializeWithAotGeneratedArtifacts(initializationContext, loggerContext)) {
			super.initialize(initializationContext, configLocation, logFile);
		}
		loggerContext.getTurboFilterList().remove(FILTER);
		markAsInitialized(loggerContext);
		if (StringUtils.hasText(System.getProperty(CONFIGURATION_FILE_PROPERTY))) {
			getLogger(LogbackLoggingSystem.class.getName()).warn("Ignoring '" + CONFIGURATION_FILE_PROPERTY
					+ "' system property. Please use 'logging.config' instead.");
		}
	}

	private boolean initializeWithAotGeneratedArtifacts(LoggingInitializationContext initializationContext,
			LoggerContext loggerContext) {
		loggerContext.reset();
		loggerContext.getStatusManager().clear();
		boolean loaded = new PatternRuleRegistry(loggerContext).load()
				&& new LogbackModel(loggerContext).load(initializationContext);
		if (loaded) {
			reportErrorsIfNecessary(loggerContext);
		}
		return loaded;
	}

	@Override
	protected void loadDefaults(LoggingInitializationContext initializationContext, LogFile logFile) {
		LoggerContext context = getLoggerContext();
		stopAndReset(context);
		boolean debug = Boolean.getBoolean("logback.debug");
		if (debug) {
			StatusListenerConfigHelper.addOnConsoleListenerInstance(context, new OnConsoleStatusListener());
		}
		Environment environment = initializationContext.getEnvironment();
		// Apply system properties directly in case the same JVM runs multiple apps
		new LogbackLoggingSystemProperties(environment, context::putProperty).apply(logFile);
		LogbackConfigurator configurator = debug ? new DebugLogbackConfigurator(context)
				: new LogbackConfigurator(context);
		new DefaultLogbackConfiguration(logFile).apply(configurator);
		context.setPackagingDataEnabled(true);
	}

	@Override
	protected void loadConfiguration(LoggingInitializationContext initializationContext, String location,
			LogFile logFile) {
		if (initializationContext != null) {
			applySystemProperties(initializationContext.getEnvironment(), logFile);
		}
		LoggerContext loggerContext = getLoggerContext();
		stopAndReset(loggerContext);
		try {
			configureByResourceUrl(initializationContext, loggerContext, ResourceUtils.getURL(location));
		}
		catch (Exception ex) {
			throw new IllegalStateException("Could not initialize Logback logging from " + location, ex);
		}
		reportErrorsIfNecessary(loggerContext);
	}

	private void reportErrorsIfNecessary(LoggerContext loggerContext) {
		List<Status> statuses = loggerContext.getStatusManager().getCopyOfStatusList();
		StringBuilder errors = new StringBuilder();
		for (Status status : statuses) {
			if (status.getLevel() == Status.ERROR) {
				errors.append((errors.length() > 0) ? String.format("%n") : "");
				errors.append(status.toString());
			}
		}
		if (errors.length() > 0) {
			throw new IllegalStateException(String.format("Logback configuration error detected: %n%s", errors));
		}
	}

	private void configureByResourceUrl(LoggingInitializationContext initializationContext, LoggerContext loggerContext,
			URL url) throws JoranException {
		if (url.toString().endsWith("xml")) {
			SpringBootJoranConfigurator configurator = new SpringBootJoranConfigurator(initializationContext);
			configurator.setContext(loggerContext);
			configurator.doConfigure(url);
			this.logbackModel = new LogbackModel(loggerContext, configurator);
		}
		else {
			new ContextInitializer(loggerContext).configureByResource(url);
		}
	}

	private void stopAndReset(LoggerContext loggerContext) {
		loggerContext.stop();
		loggerContext.reset();
		if (isBridgeHandlerInstalled()) {
			addLevelChangePropagator(loggerContext);
		}
	}

	private boolean isBridgeHandlerInstalled() {
		if (!isBridgeHandlerAvailable()) {
			return false;
		}
		java.util.logging.Logger rootLogger = LogManager.getLogManager().getLogger("");
		Handler[] handlers = rootLogger.getHandlers();
		return handlers.length == 1 && handlers[0] instanceof SLF4JBridgeHandler;
	}

	private void addLevelChangePropagator(LoggerContext loggerContext) {
		LevelChangePropagator levelChangePropagator = new LevelChangePropagator();
		levelChangePropagator.setResetJUL(true);
		levelChangePropagator.setContext(loggerContext);
		loggerContext.addListener(levelChangePropagator);
	}

	@Override
	public void cleanUp() {
		LoggerContext context = getLoggerContext();
		markAsUninitialized(context);
		super.cleanUp();
		if (isBridgeHandlerAvailable()) {
			removeJdkLoggingBridgeHandler();
		}
		context.getStatusManager().clear();
		context.getTurboFilterList().remove(FILTER);
	}

	@Override
	protected void reinitialize(LoggingInitializationContext initializationContext) {
		getLoggerContext().reset();
		getLoggerContext().getStatusManager().clear();
		loadConfiguration(initializationContext, getSelfInitializationConfig(), null);
	}

	@Override
	public List<LoggerConfiguration> getLoggerConfigurations() {
		List<LoggerConfiguration> result = new ArrayList<>();
		for (ch.qos.logback.classic.Logger logger : getLoggerContext().getLoggerList()) {
			result.add(getLoggerConfiguration(logger));
		}
		result.sort(CONFIGURATION_COMPARATOR);
		return result;
	}

	@Override
	public LoggerConfiguration getLoggerConfiguration(String loggerName) {
		String name = getLoggerName(loggerName);
		LoggerContext loggerContext = getLoggerContext();
		return getLoggerConfiguration(loggerContext.exists(name));
	}

	private String getLoggerName(String name) {
		if (!StringUtils.hasLength(name) || Logger.ROOT_LOGGER_NAME.equals(name)) {
			return ROOT_LOGGER_NAME;
		}
		return name;
	}

	private LoggerConfiguration getLoggerConfiguration(ch.qos.logback.classic.Logger logger) {
		if (logger == null) {
			return null;
		}
		LogLevel level = LEVELS.convertNativeToSystem(logger.getLevel());
		LogLevel effectiveLevel = LEVELS.convertNativeToSystem(logger.getEffectiveLevel());
		String name = getLoggerName(logger.getName());
		return new LoggerConfiguration(name, level, effectiveLevel);
	}

	@Override
	public Set<LogLevel> getSupportedLogLevels() {
		return LEVELS.getSupported();
	}

	@Override
	public void setLogLevel(String loggerName, LogLevel level) {
		ch.qos.logback.classic.Logger logger = getLogger(loggerName);
		if (logger != null) {
			logger.setLevel(LEVELS.convertSystemToNative(level));
		}
	}

	@Override
	public Runnable getShutdownHandler() {
		return () -> getLoggerContext().stop();
	}

	private ch.qos.logback.classic.Logger getLogger(String name) {
		LoggerContext factory = getLoggerContext();
		return factory.getLogger(getLoggerName(name));
	}

	private LoggerContext getLoggerContext() {
		ILoggerFactory factory = LoggerFactory.getILoggerFactory();
		Assert.isInstanceOf(LoggerContext.class, factory,
				() -> String.format(
						"LoggerFactory is not a Logback LoggerContext but Logback is on "
								+ "the classpath. Either remove Logback or the competing "
								+ "implementation (%s loaded from %s). If you are using "
								+ "WebLogic you will need to add 'org.slf4j' to "
								+ "prefer-application-packages in WEB-INF/weblogic.xml",
						factory.getClass(), getLocation(factory)));
		return (LoggerContext) factory;
	}

	private Object getLocation(ILoggerFactory factory) {
		try {
			ProtectionDomain protectionDomain = factory.getClass().getProtectionDomain();
			CodeSource codeSource = protectionDomain.getCodeSource();
			if (codeSource != null) {
				return codeSource.getLocation();
			}
		}
		catch (SecurityException ex) {
			// Unable to determine location
		}
		return "unknown location";
	}

	private boolean isAlreadyInitialized(LoggerContext loggerContext) {
		return loggerContext.getObject(LoggingSystem.class.getName()) != null;
	}

	private void markAsInitialized(LoggerContext loggerContext) {
		loggerContext.putObject(LoggingSystem.class.getName(), new Object());
	}

	private void markAsUninitialized(LoggerContext loggerContext) {
		loggerContext.removeObject(LoggingSystem.class.getName());
	}

	@Override
	public BeanFactoryInitializationAotContribution processAheadOfTime(ConfigurableListableBeanFactory beanFactory) {
		return new BeanFactoryInitializationAotContribution() {

			@Override
			public void applyTo(GenerationContext generationContext,
					BeanFactoryInitializationCode beanFactoryInitializationCode) {
				if (LogbackLoggingSystem.this.logbackModel != null) {
					LogbackLoggingSystem.this.logbackModel.save(generationContext);
					new PatternRuleRegistry(getLoggerContext()).save(generationContext);
				}
			}

		};
	}

	/**
	 * {@link LoggingSystemFactory} that returns {@link LogbackLoggingSystem} if possible.
	 */
	@Order(Ordered.LOWEST_PRECEDENCE)
	public static class Factory implements LoggingSystemFactory {

		private static final boolean PRESENT = ClassUtils.isPresent("ch.qos.logback.classic.LoggerContext",
				Factory.class.getClassLoader());

		@Override
		public LoggingSystem getLoggingSystem(ClassLoader classLoader) {
			if (PRESENT) {
				return new LogbackLoggingSystem(classLoader);
			}
			return null;
		}

	}

	private static final class PatternRuleRegistry {

		private static final String RESOURCE_LOCATION = "META-INF/spring/logback-pattern-rules";

		private final LoggerContext loggerContext;

		private PatternRuleRegistry(LoggerContext loggerContext) {
			this.loggerContext = loggerContext;
		}

		private boolean load() {
			try {
				ClassPathResource resource = new ClassPathResource(RESOURCE_LOCATION);
				if (!resource.exists()) {
					return false;
				}
				Properties properties = PropertiesLoaderUtils.loadProperties(resource);
				Map<String, String> patternRuleRegistry = getRegistryMap();
				for (String word : properties.stringPropertyNames()) {
					patternRuleRegistry.put(word, properties.getProperty(word));
				}
				return true;
			}
			catch (Exception ex) {
				throw new RuntimeException(ex);
			}
		}

		@SuppressWarnings("unchecked")
		private Map<String, String> getRegistryMap() {
			Map<String, String> patternRuleRegistry = (Map<String, String>) this.loggerContext
					.getObject(CoreConstants.PATTERN_RULE_REGISTRY);
			if (patternRuleRegistry == null) {
				patternRuleRegistry = new HashMap<>();
				this.loggerContext.putObject(CoreConstants.PATTERN_RULE_REGISTRY, patternRuleRegistry);
			}
			return patternRuleRegistry;
		}

		private void save(GenerationContext generationContext) {
			generationContext.getGeneratedFiles().addResourceFile(RESOURCE_LOCATION, this::getInputStream);
			generationContext.getRuntimeHints().resources().registerPattern(RESOURCE_LOCATION);
		}

		private InputStream getInputStream() {
			Map<String, String> patternRuleRegistry = getRegistryMap();
			Properties properties = new Properties();
			patternRuleRegistry.forEach(properties::setProperty);
			ByteArrayOutputStream bytes = new ByteArrayOutputStream();
			try {
				properties.store(bytes, "");
			}
			catch (IOException ex) {
				throw new RuntimeException(ex);
			}
			return new ByteArrayInputStream(bytes.toByteArray());
		}

	}

	private static final class LogbackModel {

		private static final String RESOURCE_LOCATION = "META-INF/spring/logback-model";

		private final LoggerContext loggerContext;

		private final DefaultNestedComponentRegistry componentRegistry;

		private final BeanDescriptionCache beanDescriptionCache;

		private Model model;

		private LogbackModel(LoggerContext loggerContext) {
			this.loggerContext = loggerContext;
			this.componentRegistry = null;
			this.beanDescriptionCache = null;
		}

		private LogbackModel(LoggerContext loggerContext, SpringBootJoranConfigurator configurator) {
			this.loggerContext = loggerContext;
			this.model = configurator.getModel();
			this.componentRegistry = configurator.getModelInterpretationContext().getDefaultNestedComponentRegistry();
			this.beanDescriptionCache = configurator.getModelInterpretationContext().getBeanDescriptionCache();
		}

		private boolean load(LoggingInitializationContext initializationContext) {
			try (InputStream modelInput = getClass().getClassLoader().getResourceAsStream(RESOURCE_LOCATION)) {
				if (modelInput == null) {
					return false;
				}
				SpringBootJoranConfigurator configurator = new SpringBootJoranConfigurator(initializationContext);
				configurator.setContext(this.loggerContext);
				try (ObjectInputStream input = new ObjectInputStream(modelInput)) {
					this.model = (Model) input.readObject();
					ModelUtil.resetForReuse(this.model);
					configurator.processModel(this.model);
					configurator.registerSafeConfiguration(this.model);
				}
			}
			catch (Exception ex) {
				throw new RuntimeException(ex);
			}
			return true;
		}

		private void save(GenerationContext generationContext) {
			ByteArrayOutputStream bytes = new ByteArrayOutputStream();
			try (ObjectOutputStream output = new ObjectOutputStream(bytes)) {
				output.writeObject(this.model);
			}
			catch (IOException ex) {
				throw new RuntimeException(ex);
			}
			Resource modelResource = new ByteArrayResource(bytes.toByteArray());
			generationContext.getGeneratedFiles().addResourceFile(RESOURCE_LOCATION, modelResource);
			generationContext.getRuntimeHints().resources().registerPattern(RESOURCE_LOCATION);
			SerializationHints serializationHints = generationContext.getRuntimeHints().serialization();
			serializationTypes(this.model).forEach(serializationHints::registerType);
			reflectionTypes(this.model).forEach((type) -> generationContext.getRuntimeHints().reflection().registerType(
					TypeReference.of(type), MemberCategory.INTROSPECT_PUBLIC_METHODS,
					MemberCategory.INVOKE_PUBLIC_METHODS, MemberCategory.INVOKE_PUBLIC_CONSTRUCTORS));
		}

		@SuppressWarnings("unchecked")
		private Set<Class<? extends Serializable>> serializationTypes(Model model) {
			Set<Class<? extends Serializable>> modelClasses = new HashSet<>();
			Class<?> candidate = model.getClass();
			while (Model.class.isAssignableFrom(candidate)) {
				if (modelClasses.add((Class<? extends Model>) candidate)) {
					ReflectionUtils.doWithFields(candidate, (field) -> {
						ReflectionUtils.makeAccessible(field);
						Object value = field.get(model);
						if (value != null) {
							Class<?> fieldType = value.getClass();
							if (Serializable.class.isAssignableFrom(fieldType)) {
								modelClasses.add((Class<? extends Serializable>) fieldType);
							}
						}
					});
					candidate = candidate.getSuperclass();
				}
			}
			for (Model submodel : model.getSubModels()) {
				modelClasses.addAll(serializationTypes(submodel));
			}
			return modelClasses;
		}

		private Set<String> reflectionTypes(Model model) {
			Set<String> reflectionTypes = new HashSet<>();
			if (model instanceof ComponentModel) {
				String className = ((ComponentModel) model).getClassName();
				processComponent(className, reflectionTypes);
			}
			String tag = model.getTag();
			if (tag != null) {
				String componentType = this.componentRegistry.findDefaultComponentTypeByTag(tag);
				processComponent(componentType, reflectionTypes);
			}
			for (Model submodel : model.getSubModels()) {
				reflectionTypes.addAll(reflectionTypes(submodel));
			}
			return reflectionTypes;
		}

		private void processComponent(String componentType, Set<String> reflectionTypes) {
			if (componentType != null) {
				try {
					BeanDescription beanDescription = this.beanDescriptionCache
							.getBeanDescription(ClassUtils.forName(componentType, getClass().getClassLoader()));
					processComponentMethods(beanDescription.getPropertyNameToAdder(), reflectionTypes);
					processComponentMethods(beanDescription.getPropertyNameToSetter(), reflectionTypes);
				}
				catch (Throwable ex) {
					throw new RuntimeException(ex);
				}
				reflectionTypes.add(componentType);
			}
		}

		private void processComponentMethods(Map<String, Method> methods, Set<String> reflectionTypes) {
			methods.forEach((name, method) -> {
				for (Class<?> parameterType : method.getParameterTypes()) {
					reflectionTypes.add(parameterType.getName());
				}
			});
		}

	}

}
