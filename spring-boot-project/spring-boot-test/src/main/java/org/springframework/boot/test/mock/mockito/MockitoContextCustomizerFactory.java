/*
 * Copyright 2012-2021 the original author or authors.
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

package org.springframework.boot.test.mock.mockito;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.springframework.test.context.ContextConfigurationAttributes;
import org.springframework.test.context.ContextCustomizer;
import org.springframework.test.context.ContextCustomizerFactory;
import org.springframework.test.context.TestContextAnnotationUtils;
import org.springframework.util.CollectionUtils;

/**
 * A {@link ContextCustomizerFactory} to add Mockito support.
 *
 * @author Phillip Webb
 */
class MockitoContextCustomizerFactory implements ContextCustomizerFactory {

	@Override
	public ContextCustomizer createContextCustomizer(Class<?> testClass,
			List<ContextConfigurationAttributes> configAttributes) {
		// We gather the explicit mock definitions here since they form part of the
		// MergedContextConfiguration key. Different mocks need to have a different key.
		String contextName = determineContextName(configAttributes);
		DefinitionsParser parser = new DefinitionsParser(contextName);
		parseDefinitions(testClass, parser);
		return new MockitoContextCustomizer(parser.getDefinitions(), contextName);
	}

	private String determineContextName(List<ContextConfigurationAttributes> configAttributes) {
		if (CollectionUtils.isEmpty(configAttributes)) {
			return null;
		}
		Set<String> names = new HashSet<>();
		for (ContextConfigurationAttributes attributes : configAttributes) {
			String name = attributes.getName();
			if (name != null) {
				names.add(name);
			}
		}
		if (names.size() > 1) {
			throw new IllegalStateException("Multiple different context names specified: " + names);
		}
		return names.isEmpty() ? null : names.iterator().next();
	}

	private void parseDefinitions(Class<?> testClass, DefinitionsParser parser) {
		parser.parse(testClass);
		if (TestContextAnnotationUtils.searchEnclosingClass(testClass)) {
			parseDefinitions(testClass.getEnclosingClass(), parser);
		}
	}

}
