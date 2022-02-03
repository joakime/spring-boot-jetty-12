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

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * @author Andy Wilkinson
 */
public interface SpringBootFactoriesLoader {

	/**
	 * The location to look for factories.
	 * <p>
	 * Can be present in multiple JAR files.
	 */
	String FACTORIES_RESOURCE_LOCATION = "META-INF/spring-boot.factories";

	static SpringBootFactoriesLoader from(ClassLoader classLoader) {
		return from(classLoader, new ThrowingFailureHandler());
	}

	static SpringBootFactoriesLoader from(ClassLoader classLoader, FailureHandler failureHandler) {
		// Perhaps we could load a different implementation here
		// Alternatively, should InstantiatorSpringBootFactoriesLoader
		// be aware of AOT and know how to load pre-generated "providers"
		// that can create something named in the factories file?
		return new InstantiatorSpringBootFactoriesLoader(classLoader, failureHandler);
	}

	default <T> List<T> loadFactories(Class<T> type) {
		return loadFactories(type, new FactoryArguments());
	}

	<T> List<T> loadFactories(Class<T> type, FactoryArguments arguments);

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

	interface FailureHandler {

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
