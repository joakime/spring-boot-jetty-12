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

package org.springframework.boot.autoconfigure.condition;

import org.hibernate.validator.HibernateValidator;
import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.boot.testsupport.classpath.ClassPathExclusions;
import org.springframework.core.env.Environment;
import org.springframework.core.io.ResourceLoader;

import static org.assertj.core.api.Assertions.assertThat;

@ClassPathExclusions("hibernate-validator-*.jar")
public class OnClassRegistrationPredicateTests {

	private final RegistrationContext context = new TestRegistrationContext();

	@Test
	void classPresent() {
		ConditionOutcome outcome = new OnClassRegistrationPredicate("here", () -> Object.class)
				.getMatchOutcome(this.context);
		assertThat(outcome.isMatch()).isTrue();
		assertThat(outcome.getMessage()).isEqualTo("OnClassRegistrationPredicate found class java.lang.Object");
	}

	@Test
	void classMissing() {
		ConditionOutcome outcome = new OnClassRegistrationPredicate("here", () -> HibernateValidator.class)
				.getMatchOutcome(this.context);
		assertThat(outcome.isMatch()).isFalse();
		assertThat(outcome.getMessage()).isEqualTo(
				"OnClassRegistrationPredicate did not find class org.hibernate.validator.HibernateValidator");
	}

	@Test
	void superClassMissing() {
		ConditionOutcome outcome = new OnClassRegistrationPredicate("here", () -> CustomHibernateValidator.class)
				.getMatchOutcome(this.context);
		assertThat(outcome.isMatch()).isFalse();
		assertThat(outcome.getMessage()).isEqualTo(
				"OnClassRegistrationPredicate did not find class org.springframework.boot.autoconfigure.condition.OnClassFunctionalConditionTests.CustomHibernateValidator");
	}

	static class CustomHibernateValidator extends HibernateValidator {

	}

	static class TestRegistrationContext implements RegistrationContext {

		@Override
		public ConfigurableListableBeanFactory getBeanFactory() {
			throw new UnsupportedOperationException("Auto-generated method stub");
		}

		@Override
		public Environment getEnvironment() {
			throw new UnsupportedOperationException("Auto-generated method stub");
		}

		@Override
		public ResourceLoader getResourceLoader() {
			throw new UnsupportedOperationException("Auto-generated method stub");
		}

		@Override
		public ClassLoader getClassLoader() {
			return TestRegistrationContext.class.getClassLoader();
		}

	}

}
