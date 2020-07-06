/*
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
 */

package org.springframework.boot.autoconfigure.context;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.beans.factory.support.BeanDefinitionRegistryPostProcessor;
import org.springframework.boot.autoconfigure.StandardBeanRegistry;
import org.springframework.boot.test.util.TestPropertyValues;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.support.PropertySourcesPlaceholderConfigurer;
import org.springframework.util.StringUtils;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link PropertyPlaceholderAutoConfigurationBeanRegistrar}.
 *
 * @author Dave Syer
 */
class PropertyPlaceholderAutoConfigurationBeanRegistrarTests {

	private final AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext();

	@AfterEach
	void close() {
		if (this.context != null) {
			this.context.close();
		}
	}

	@Test
	void propertyPlaceholders() {
		TestPropertyValues.of("foo:two").applyTo(this.context);
		this.context.register(PlaceholderConfig.class);
		new PropertyPlaceholderAutoConfigurationBeanRegistrar().registerBeans(new StandardBeanRegistry(this.context));
		this.context.refresh();
		assertThat(this.context.getBean(PlaceholderConfig.class).getFoo()).isEqualTo("two");
	}

	@Test
	void propertyPlaceholdersOverride() {
		TestPropertyValues.of("foo:two").applyTo(this.context);
		this.context.register(PlaceholderConfig.class, PlaceholdersOverride.class);
		this.context.registerBean(BeanRegistrarThingy.class,
				(definition) -> definition.getConstructorArgumentValues().addGenericArgumentValue(this.context));
		new PropertyPlaceholderAutoConfigurationBeanRegistrar().registerBeans(new StandardBeanRegistry(this.context));
		this.context.refresh();
		assertThat(this.context.getBean(PlaceholderConfig.class).getFoo()).isEqualTo("spam");
	}

	@Configuration(proxyBeanMethods = false)
	static class PlaceholderConfig {

		@Value("${foo:bar}")
		private String foo;

		String getFoo() {
			return this.foo;
		}

	}

	@Configuration(proxyBeanMethods = false)
	static class PlaceholdersOverride {

		@Bean
		static PropertySourcesPlaceholderConfigurer morePlaceholders() {
			PropertySourcesPlaceholderConfigurer configurer = new PropertySourcesPlaceholderConfigurer();
			configurer.setProperties(StringUtils.splitArrayElementsIntoProperties(new String[] { "foo=spam" }, "="));
			configurer.setLocalOverride(true);
			configurer.setOrder(0);
			return configurer;
		}

	}

	static class BeanRegistrarThingy implements BeanDefinitionRegistryPostProcessor {

		@Override
		public void postProcessBeanFactory(ConfigurableListableBeanFactory beanFactory) throws BeansException {
			System.out.println(beanFactory);
		}

		@Override
		public void postProcessBeanDefinitionRegistry(BeanDefinitionRegistry registry) throws BeansException {
			System.out.println(registry);
		}

	}

}
