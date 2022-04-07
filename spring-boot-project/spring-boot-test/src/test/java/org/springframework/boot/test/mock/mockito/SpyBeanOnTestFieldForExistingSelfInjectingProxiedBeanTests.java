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

package org.springframework.boot.test.mock.mockito;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mockito;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.BeanFactoryPostProcessor;
import org.springframework.beans.factory.support.AbstractAutowireCapableBeanFactory;
import org.springframework.boot.test.mock.mockito.SpyBeanOnTestFieldForExistingSelfInjectingProxiedBeanTests.Config;
import org.springframework.boot.test.mock.mockito.SpyBeanOnTestFieldForExistingSelfInjectingProxiedBeanTests.SelfInjecting;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.transaction.annotation.EnableTransactionManagement;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Test that {@link SpyBean @SpyBean} on a test class field can be used to replace an
 * existing bean that is proxied and injects itself.
 *
 * @author Andy Wilkinson
 */
@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = { Config.class, SelfInjecting.class })
class SpyBeanOnTestFieldForExistingSelfInjectingProxiedBeanTests {

	@SpyBean
	private SelfInjecting selfInjecting;

	@Test
	void selfInjectingProxiedBeanCanBeSpied() {
		assertThat(Mockito.mockingDetails(this.selfInjecting.getSelf()).isSpy()).isTrue();
		assertThat(Mockito.mockingDetails(this.selfInjecting).isSpy()).isTrue();
	}

	@Configuration
	@EnableTransactionManagement
	static class Config {

		@Bean
		static BeanFactoryPostProcessor enableRawInjection() {
			return (beanFactory) -> ((AbstractAutowireCapableBeanFactory) beanFactory)
					.setAllowRawInjectionDespiteWrapping(true);
		}

	}

	static class SelfInjecting {

		@Autowired
		private SelfInjecting self;

		@Transactional
		public void someMethod() {

		}

		SelfInjecting getSelf() {
			return this.self;
		}

	}

}
