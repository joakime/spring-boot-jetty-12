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
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.springframework.core.annotation.MergedAnnotation;
import org.springframework.core.annotation.MergedAnnotations;
import org.springframework.test.context.ContextConfigurationAttributes;
import org.springframework.test.context.ContextCustomizer;
import org.springframework.test.context.ContextCustomizerFactory;
import org.springframework.util.ReflectionUtils;

/**
 * {@link ContextCustomizerFactory} to support property extraction.
 *
 * @author Andy Wilkinson
 * @since 3.1.0
 */
class PropertiesExtractionContextCustomizerFactory implements ContextCustomizerFactory {

	@Override
	public ContextCustomizer createContextCustomizer(Class<?> testClass,
			List<ContextConfigurationAttributes> configAttributes) {
		Set<Field> fields = new HashSet<>();
		findFields(testClass, fields);
		if (fields.isEmpty()) {
			return null;
		}
		return new PropertiesExtractionContextCustomizer(fields);
	}

	private void findFields(Class<?> testClass, Set<Field> fields) {
		ReflectionUtils.doWithLocalFields(testClass, (field) -> {
			MergedAnnotations annotations = MergedAnnotations.from(field);
			MergedAnnotation<ConfigurationPropertiesSource> configurationPropertiesSource = annotations
				.get(ConfigurationPropertiesSource.class);
			if (configurationPropertiesSource.isPresent()) {
				fields.add(field);
			}
			if (testClass.getSuperclass() != null && !Object.class.equals(testClass.getSuperclass())) {
				findFields(testClass.getSuperclass(), fields);
			}
		});
	}

}
