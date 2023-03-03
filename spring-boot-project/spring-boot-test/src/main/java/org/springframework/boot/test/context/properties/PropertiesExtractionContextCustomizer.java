/*
 * Copyright 2012-2023 the original author or authors.
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

package org.springframework.boot.test.context.properties;

import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.springframework.boot.env.OriginTrackedMapPropertySource;
import org.springframework.boot.origin.Origin;
import org.springframework.boot.origin.OriginTrackedValue;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.core.annotation.AnnotationAwareOrderComparator;
import org.springframework.core.io.support.SpringFactoriesLoader;
import org.springframework.test.context.ContextCustomizer;
import org.springframework.test.context.MergedContextConfiguration;
import org.springframework.util.ReflectionUtils;

/**
 * {@link ContextCustomizer} to support
 * {@link ConfigurationPropertiesSource @ConfigurationPropertiesSource}-annotated fields.
 *
 * @author Andy Wilkinson
 */
@SuppressWarnings("rawtypes")
class PropertiesExtractionContextCustomizer implements ContextCustomizer {

	private final Set<Field> fields;

	private final List<PropertiesExtractor> extractors;

	PropertiesExtractionContextCustomizer(Set<Field> fields) {
		this.fields = fields;
		List<PropertiesExtractor> extractors = SpringFactoriesLoader
			.forDefaultResourceLocation(getClass().getClassLoader())
			.load(PropertiesExtractor.class);
		AnnotationAwareOrderComparator.sort(extractors);
		this.extractors = extractors;
	}

	@Override
	@SuppressWarnings("unchecked")
	public void customizeContext(ConfigurableApplicationContext context, MergedContextConfiguration mergedConfig) {
		Map<String, Object> properties = new HashMap<>();
		for (Field field : this.fields) {
			ReflectionUtils.makeAccessible(field);
			Object source = ReflectionUtils.getField(field, null);
			for (PropertiesExtractor extractor : this.extractors) {
				Class<?> sourceType = presentSourceType(extractor);
				if (sourceType != null && sourceType.isInstance(source)) {
					Map<String, Object> extracted = extractor.extract(source);
					if (extracted != null) {
						extracted.forEach((key, value) -> {
							OriginTrackedValue originTrackedValue = OriginTrackedValue.of(value, Origin.from(field));
							Object existingValue = properties.putIfAbsent(key, originTrackedValue);
							if (existingValue != null) {
								throw new IllegalStateException(
										"Property '" + key + "' has already been extracted from '"
												+ ((OriginTrackedValue) existingValue).getOrigin() + "'");
							}
						});
					}
				}
			}
		}
		context.getEnvironment()
			.getPropertySources()
			.addFirst(new OriginTrackedMapPropertySource("extractedProperties", properties));
	}

	private Class<?> presentSourceType(PropertiesExtractor extractor) {
		try {
			return extractor.sourceType();
		}
		catch (Throwable ex) {
			return null;
		}
	}

}
