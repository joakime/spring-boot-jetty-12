/*
 * Copyright 2012-2020 the original author or authors.
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

package org.springframework.boot.context.properties;

import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.boot.validation.beanvalidation.MethodValidationExclusionFilter;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.util.Assert;

/**
 * {@link MethodValidationExclusionFilter} that excludes classes annotated with
 * {@link ConfigurationProperties} from method validation.
 *
 * @author Andy Wilkinson
 */
class ConfigurationPropertiesMethodValidationExclusionFilter implements MethodValidationExclusionFilter {

	private static final String BEAN_NAME = ConfigurationPropertiesMethodValidationExclusionFilter.class.getName();

	@Override
	public boolean exclude(Class<?> type) {
		return AnnotatedElementUtils.hasAnnotation(type, ConfigurationProperties.class);
	}

	static void register(BeanDefinitionRegistry registry) {
		Assert.notNull(registry, "Registry must not be null");
		if (!registry.containsBeanDefinition(BEAN_NAME)) {
			BeanDefinition definition = BeanDefinitionBuilder
					.genericBeanDefinition(ConfigurationPropertiesMethodValidationExclusionFilter.class,
							ConfigurationPropertiesMethodValidationExclusionFilter::new)
					.getBeanDefinition();
			registry.registerBeanDefinition(BEAN_NAME, definition);
		}
	}

}
