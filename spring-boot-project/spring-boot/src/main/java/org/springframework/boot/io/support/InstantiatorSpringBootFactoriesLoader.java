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
import java.util.stream.Collectors;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.boot.util.Instantiator;
import org.springframework.core.annotation.AnnotationAwareOrderComparator;
import org.springframework.core.io.UrlResource;
import org.springframework.core.io.support.PropertiesLoaderUtils;
import org.springframework.core.io.support.SpringFactoriesLoader;
import org.springframework.util.Assert;
import org.springframework.util.ConcurrentReferenceHashMap;
import org.springframework.util.StringUtils;

/**
 * A {@link SpringBootFactoriesLoader} that uses an {@link Instantiator}.
 *
 * @author Andy Wilkinson
 */
class InstantiatorSpringBootFactoriesLoader implements SpringBootFactoriesLoader {

	private static final Map<ClassLoader, Map<String, List<String>>> cache = new ConcurrentReferenceHashMap<>();

	private static final Log logger = LogFactory.getLog(InstantiatorSpringBootFactoriesLoader.class);

	private final ClassLoader classLoader;

	private final FailureHandler failureHandler;

	InstantiatorSpringBootFactoriesLoader(ClassLoader classLoader, FailureHandler failureHandler) {
		this.classLoader = classLoader != null ? classLoader : SpringBootFactoriesLoader.class.getClassLoader();
		this.failureHandler = failureHandler;
	}

	@Override
	public <T> List<T> loadFactories(Class<T> factoryType, FactoryArguments arguments) {
		Assert.notNull(factoryType, "'factoryType' must not be null");
		Assert.notNull(arguments, "'arguments' must not be null");
		List<String> factoryImplementationNames = loadFactoryNames(factoryType);
		if (logger.isTraceEnabled()) {
			logger.trace("Loaded [" + factoryType.getName() + "] names: " + factoryImplementationNames);
		}
		Instantiator<T> instantiator = new Instantiator<>(factoryType, (parameters) -> {
			arguments.getArguments().forEach(parameters::add);
		});
		List<T> instances = new ArrayList<>();
		// This would be less clunky if we pushed the failure handler down into the
		// instantiator
		for (String name : factoryImplementationNames) {
			try {
				instances.addAll(instantiator.instantiate(this.classLoader, List.of(name)));
			}
			catch (Throwable ex) {
				this.failureHandler.handleFailure(ex, factoryType, name);
			}
		}
		AnnotationAwareOrderComparator.sort(instances);
		return instances;
	}

	private static Map<String, List<String>> loadSpringFactories(ClassLoader classLoader) {
		Map<String, List<String>> result = cache.get(classLoader);
		if (result != null) {
			return result;
		}

		result = new HashMap<>();
		try {
			Enumeration<URL> urls = classLoader.getResources(FACTORIES_RESOURCE_LOCATION);
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
			result.replaceAll((factoryType, implementations) -> implementations.stream().distinct()
					.collect(Collectors.collectingAndThen(Collectors.toList(), Collections::unmodifiableList)));
			cache.put(classLoader, result);
		}
		catch (IOException ex) {
			throw new IllegalArgumentException(
					"Unable to load factories from location [" + FACTORIES_RESOURCE_LOCATION + "]", ex);
		}
		return result;
	}

	private List<String> loadFactoryNames(Class<?> factoryType) {
		ClassLoader classLoaderToUse = this.classLoader;
		if (classLoaderToUse == null) {
			classLoaderToUse = SpringFactoriesLoader.class.getClassLoader();
		}
		String factoryTypeName = factoryType.getName();
		return loadSpringFactories(classLoaderToUse).getOrDefault(factoryTypeName, Collections.emptyList());
	}

}
