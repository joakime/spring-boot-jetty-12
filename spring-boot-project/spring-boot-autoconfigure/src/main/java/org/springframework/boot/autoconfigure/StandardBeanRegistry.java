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

package org.springframework.boot.autoconfigure;

import java.util.Stack;
import java.util.function.Consumer;

import org.springframework.boot.autoconfigure.condition.On;
import org.springframework.boot.autoconfigure.condition.RegistrationContext;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.support.GenericApplicationContext;
import org.springframework.util.StringUtils;

public class StandardBeanRegistry implements BeanRegistry {

	private final ConfigurableApplicationContext context;

	private final RegistrationContext registrationContext;

	private final Stack<String> locations = new Stack<>();

	public StandardBeanRegistry(ConfigurableApplicationContext applicationContext) {
		this.context = applicationContext;
		this.registrationContext = new ApplicationContextRegistrationContext(applicationContext);
	}

	@Override
	public void registerBean(Class<?> beanClass) {
		((GenericApplicationContext) this.context).registerBean(beanClass);
	}

	@Override
	public RegistrationContext getRegistrationContext() {
		return this.registrationContext;
	}

	@Override
	public void conditional(String location, Consumer<On> consumer, Runnable action) {
		this.locations.push(location);
		On on = new On(StringUtils.collectionToDelimitedString(this.locations, ""));
		consumer.accept(on);
		if (on.getPredicate().test(this.registrationContext)) {
			action.run();
		}
		this.locations.pop();
	}

}