/*
 *
 * Copyright 2012-2019 the original author or authors.
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
 *
 */

package org.springframework.boot.context.properties.bind;

import java.lang.reflect.Method;

import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.boot.context.properties.source.ConfigurationProperty;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

/**
 * @author Madhura Bhave
 */
public class BoundPropertiesHolder {

	private static final String BEAN_NAME = "boundPropertiesHolder";

	private MultiValueMap<Method, ConfigurationProperty> properties = new LinkedMultiValueMap<>();

	public MultiValueMap<Method, ConfigurationProperty> getProperties() {
		return this.properties;
	}

	void recordBinding(ConfigurationProperty configurationProperty, Method getter) {
		this.properties.add(getter, configurationProperty);
	}

	public static BoundPropertiesHolder get(ConfigurableListableBeanFactory beanFactory) {
		synchronized (beanFactory) {
			BoundPropertiesHolder report;
			if (beanFactory.containsSingleton(BEAN_NAME)) {
				report = beanFactory.getBean(BEAN_NAME, BoundPropertiesHolder.class);
			}
			else {
				report = new BoundPropertiesHolder();
				beanFactory.registerSingleton(BEAN_NAME, report);
			}
			return report;
		}
	}

}
