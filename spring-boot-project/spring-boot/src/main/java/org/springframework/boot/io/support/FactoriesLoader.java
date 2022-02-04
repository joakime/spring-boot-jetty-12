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

package org.springframework.boot.io.support;

import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.ServiceLoader;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.core.annotation.AnnotationAwareOrderComparator;
import org.springframework.core.io.UrlResource;
import org.springframework.core.io.support.PropertiesLoaderUtils;
import org.springframework.util.ClassUtils;
import org.springframework.util.ConcurrentReferenceHashMap;
import org.springframework.util.StringUtils;

/**
 * @author awilkinson
 */
public class FactoriesLoader {

	public static final String FACTORIES_RESOURCE_LOCATION = "META-INF/spring.factories";

	private static final Map<ClassLoader, Map<String, Map<String, FactoryLoader<?>>>> cache = new ConcurrentReferenceHashMap<>();

	private final ClassLoader classLoader;

	public FactoriesLoader(ClassLoader classLoader) {
		this.classLoader = classLoader;
	}

	public <T> List<T> load(Class<T> type) {
		return load(type, new FactoryArguments());
	}

	public <T> List<T> load(Class<T> type, FactoryArguments arguments) {
		return load(type, arguments, new ThrowingFailureHandler());
	}

	public <T> List<T> load(Class<T> type, FailureHandler failureHandler) {
		return load(type, new FactoryArguments(), failureHandler);
	}

	@SuppressWarnings("unchecked")
	public <T> List<T> load(Class<T> type, FactoryArguments arguments, FailureHandler failureHandler) {
		Map<String, FactoryLoader<?>> loadersForType = getLoaders().get(type.getName());
		if (loadersForType == null) {
			return Collections.emptyList();
		}
		List<T> factories = new ArrayList<>();
		for (Map.Entry<String, FactoryLoader<?>> idAndLoader : loadersForType.entrySet()) {
			try {
				factories.add((T) idAndLoader.getValue().load(arguments));
			}
			catch (Throwable ex) {
				failureHandler.handleFailure(ex, type, idAndLoader.getKey());
			}
		}
		AnnotationAwareOrderComparator.sort(factories);
		return factories;
	}

	private Map<String, Map<String, FactoryLoader<?>>> getLoaders() {
		return cache.computeIfAbsent(this.classLoader, (cl) -> loadLoaders());
	}

	private Map<String, Map<String, FactoryLoader<?>>> loadLoaders() {
		Map<String, Map<String, FactoryLoader<?>>> loaders = loadServiceBasedLoaders();
		Map<String, List<String>> springFactories = loadSpringFactories();
		for (Map.Entry<String, List<String>> typeAndFactoryNames : springFactories.entrySet()) {
			Map<String, FactoryLoader<?>> loadersForType = loaders.computeIfAbsent(typeAndFactoryNames.getKey(),
					(key) -> new HashMap<>());
			for (String id : typeAndFactoryNames.getValue()) {
				loadersForType.computeIfAbsent(id,
						(key) -> instantiatorFactoryLoader(typeAndFactoryNames.getKey(), id));
			}
		}
		return loaders;
	}

	private Map<String, Map<String, FactoryLoader<?>>> loadServiceBasedLoaders() {
		Map<String, Map<String, FactoryLoader<?>>> loaders = new HashMap<>();
		for (FactoryLoader<?> loader : ServiceLoader.load(FactoryLoader.class, this.classLoader)) {
			Map<String, FactoryLoader<?>> loadersForType = loaders.computeIfAbsent(loader.type().getName(),
					(key) -> new HashMap<>());
			loadersForType.computeIfAbsent(loader.id(), (key) -> loader);
		}
		return loaders;
	}

	private Map<String, List<String>> loadSpringFactories() {
		Map<String, List<String>> result = new HashMap<>();
		try {
			Enumeration<URL> urls = this.classLoader.getResources(FACTORIES_RESOURCE_LOCATION);
			while (urls.hasMoreElements()) {
				URL url = urls.nextElement();
				UrlResource resource = new UrlResource(url);
				Properties properties = PropertiesLoaderUtils.loadProperties(resource);
				for (Map.Entry<?, ?> entry : properties.entrySet()) {
					String factoryTypeName = ((String) entry.getKey()).trim();
					String[] factoryImplementationNames = StringUtils
							.commaDelimitedListToStringArray((String) entry.getValue());
					for (String factoryImplementationName : factoryImplementationNames) {
						result.computeIfAbsent(factoryTypeName, key -> new ArrayList<>())
								.add(factoryImplementationName.trim());
					}
				}
			}
			// Replace all lists with unmodifiable lists containing unique elements
			result.replaceAll((factoryType, implementations) -> implementations.stream().distinct().toList());
		}
		catch (IOException ex) {
			throw new IllegalArgumentException(
					"Unable to load factories from location [" + FACTORIES_RESOURCE_LOCATION + "]", ex);
		}
		return result;
	}

	private FactoryLoader<?> instantiatorFactoryLoader(String typeName, String id) {
		return new InstantiatorFactoryLoader<>(loadFactoryType(typeName), id);
	}

	private Class<?> loadFactoryType(String typeName) {
		try {
			return ClassUtils.forName(typeName, this.classLoader);
		}
		catch (Throwable ex) {
			throw new IllegalStateException(
					"Failed to load '" + typeName + "' with class loader '" + this.classLoader + "'");
		}
	}

	public static class FactoryArguments {

		private Map<Class<?>, Object> arguments = new HashMap<>();

		public <T> void add(Class<T> type, T value) {
			this.arguments.put(type, value);
		}

		@SuppressWarnings("unchecked")
		public <T> T get(Class<T> type) {
			return (T) this.arguments.get(type);
		}

		Map<Class<?>, Object> getArguments() {
			return this.arguments;
		}

	}

	public interface FailureHandler {

		void handleFailure(Throwable failure, Class<?> type, String factoryName);

	}

	public static class ThrowingFailureHandler implements FailureHandler {

		@Override
		public void handleFailure(Throwable failure, Class<?> type, String factoryName) {
			throw new IllegalStateException("Failed to load factory '" + factoryName + "' as '" + type.getName(),
					failure);
		}

	}

	public static class LoggingFailureHandler implements FailureHandler {

		private static final Log log = LogFactory.getLog(LoggingFailureHandler.class);

		@Override
		public void handleFailure(Throwable failure, Class<?> type, String factoryName) {
			if (log.isTraceEnabled()) {
				log.trace("Failed to load factory '" + factoryName + "' as a '" + type.getName() + "'", failure);
			}
		}

	}

}
