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

import java.util.function.Consumer;
import java.util.function.Predicate;

import org.springframework.boot.autoconfigure.condition.On;
import org.springframework.boot.autoconfigure.condition.RegistrationContext;

/**
 * @author awilkinson
 */
public interface BeanRegistry {

	void registerBean(Class<?> beanClass);

	void conditional(String location, Consumer<On> consumer, Runnable action);

	public RegistrationContext getRegistrationContext();

	public static class ConditionalRegistration {

		private final Predicate<RegistrationContext> predicate;

		private BeanRegistrar delegate;

		public ConditionalRegistration(Predicate<RegistrationContext> predicate) {
			this.predicate = predicate;
		}

		void registerBeans(RegistrationContext context, BeanRegistry registry) {
			if (this.predicate.test(context)) {
				this.delegate.registerBeans(registry);
			}
		}

		public ConditionalRegistration then(Runnable then) {
			this.delegate = (registry) -> then.run();
			return this;
		}

	}

}
