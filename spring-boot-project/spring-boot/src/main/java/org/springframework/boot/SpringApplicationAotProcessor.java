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

package org.springframework.boot;

import java.lang.reflect.Method;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.boot.SpringApplication.AbandonedRunException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.aot.ContextAotProcessor;
import org.springframework.context.support.GenericApplicationContext;
import org.springframework.core.io.support.SpringFactoriesLoader;
import org.springframework.core.io.support.SpringFactoriesLoader.ArgumentResolver;
import org.springframework.util.Assert;
import org.springframework.util.ReflectionUtils;
import org.springframework.util.StringUtils;
import org.springframework.util.function.ThrowingSupplier;

/**
 * Entry point for AOT processing of a {@link SpringApplication}.
 * <p>
 * <strong>For internal use only.</strong>
 *
 * @author Stephane Nicoll
 * @author Andy Wilkinson
 * @author Phillip Webb
 * @since 3.0.0
 */
public class SpringApplicationAotProcessor extends ContextAotProcessor {

	private static final Log logger = LogFactory.getLog(SpringApplicationAotProcessor.class);

	private final String[] applicationArgs;

	/**
	 * Create a new processor for the specified application and settings.
	 * @param application the application main class
	 * @param settings the general AOT processor settings
	 * @param applicationArgs the arguments to provide to the main method
	 */
	public SpringApplicationAotProcessor(Class<?> application, Settings settings, String[] applicationArgs) {
		super(application, settings);
		this.applicationArgs = applicationArgs;
	}

	@Override
	protected GenericApplicationContext prepareApplicationContext(Class<?> application) {
		return new AotProcessorHook(application).run(() -> {
			Method mainMethod = application.getMethod("main", String[].class);
			return ReflectionUtils.invokeMethod(mainMethod, null, new Object[] { this.applicationArgs });
		});
	}

	public static void main(String[] args) throws Exception {
		int requiredArgs = 6;
		Assert.isTrue(args.length >= requiredArgs, () -> "Usage: " + SpringApplicationAotProcessor.class.getName()
				+ " <applicationName> <sourceOutput> <resourceOutput> <classOutput> <groupId> <artifactId> <originalArgs...>");
		Class<?> application = Class.forName(args[0]);
		Settings settings = Settings.builder().sourceOutput(Paths.get(args[1])).resourceOutput(Paths.get(args[2]))
				.classOutput(Paths.get(args[3])).groupId((StringUtils.hasText(args[4])) ? args[4] : "unspecified")
				.artifactId(args[5]).build();
		String[] applicationArgs = (args.length > requiredArgs) ? Arrays.copyOfRange(args, requiredArgs, args.length)
				: new String[0];
		try {
			new SpringApplicationAotProcessor(application, settings, applicationArgs).process();
		}
		catch (Exception ex) {
			reportFailure(getExceptionReporters(), ex);
			ReflectionUtils.rethrowRuntimeException(ex);
		}
	}

	private static Collection<SpringBootExceptionReporter> getExceptionReporters() {
		try {
			return SpringFactoriesLoader
					.forDefaultResourceLocation(SpringApplicationAotProcessor.class.getClassLoader())
					.load(SpringBootExceptionReporter.class, (ArgumentResolver) null);
		}
		catch (Throwable ex) {
			return Collections.emptyList();
		}
	}

	private static void reportFailure(Collection<SpringBootExceptionReporter> exceptionReporters, Throwable failure) {
		try {
			for (SpringBootExceptionReporter reporter : exceptionReporters) {
				if (reporter.reportException(failure)) {
					registerLoggedException(failure);
					return;
				}
			}
		}
		catch (Throwable ex) {
			// Continue with normal handling of the original failure
		}
		if (logger.isErrorEnabled()) {
			logger.error("AOT processing failed", failure);
			registerLoggedException(failure);
		}
	}

	private static void registerLoggedException(Throwable exception) {
		SpringBootExceptionHandler handler = SpringBootExceptionHandler.forCurrentThread();
		if (handler != null) {
			handler.registerLoggedException(exception);
		}
	}

	/**
	 * {@link SpringApplicationHook} used to capture the {@link ApplicationContext} and
	 * trigger early exit of main method.
	 */
	private static final class AotProcessorHook implements SpringApplicationHook {

		private final Class<?> application;

		private AotProcessorHook(Class<?> application) {
			this.application = application;
		}

		@Override
		public SpringApplicationRunListener getRunListener(SpringApplication application) {
			return new SpringApplicationRunListener() {

				@Override
				public void contextLoaded(ConfigurableApplicationContext context) {
					throw new AbandonedRunException(context);
				}

			};
		}

		private <T> GenericApplicationContext run(ThrowingSupplier<T> action) {
			try {
				SpringApplication.withHook(this, action);
			}
			catch (AbandonedRunException ex) {
				ApplicationContext context = ex.getApplicationContext();
				Assert.isInstanceOf(GenericApplicationContext.class, context,
						() -> "AOT processing requires a GenericApplicationContext but got a "
								+ context.getClass().getName());
				return (GenericApplicationContext) context;
			}
			throw new IllegalStateException(
					"No application context available after calling main method of '%s'. Does it run a SpringApplication?"
							.formatted(this.application.getName()));
		}

	}

}
