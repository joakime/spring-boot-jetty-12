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

package org.springframework.boot.autoconfigure.mustache;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Stream;

import com.samskivert.mustache.Mustache.Compiler;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.autoconfigure.context.PropertyPlaceholderAutoConfiguration;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.MapPropertySource;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration Tests for {@link MustacheAutoConfiguration} outside of a web application.
 *
 * @author Dave Syer
 * @author Andy Wilkinson
 */
class MustacheStandaloneIntegrationTests {

	private final ApplicationContextRunner contextRunner = new ApplicationContextRunner().withConfiguration(
			AutoConfigurations.of(MustacheAutoConfiguration.class, PropertyPlaceholderAutoConfiguration.class));

	@MapAndObjectModelsTest
	void whenTopLevelPropertyIsInTheEnvironmentAndNotInModelThenEnvironmentValueIsUsed(Object model) {
		this.contextRunner.withPropertyValues("absent=Environment").run((context) -> {
			Compiler compiler = context.getBean(Compiler.class);
			assertThat(compiler.compile("Hello: {{absent}}").execute(model)).isEqualTo("Hello: Environment");
		});
	}

	@MapAndObjectModelsTest
	void whenTopLevelPropertyIsInTheEnvironmentAndInModelThenModelValueIsUsed(Object model) {
		this.contextRunner.withPropertyValues("name=Environment").run((context) -> {
			Compiler compiler = context.getBean(Compiler.class);
			assertThat(compiler.compile("Hello: {{name}}").execute(model)).isEqualTo("Hello: Model");
		});
	}

	@MapAndObjectModelsTest
	void whenNestedPropertyIsInTheEnvironmentAndNotInObjectModelThenEnvironmentValueIsUsed(Object model) {
		this.contextRunner.withPropertyValues("nested.absent=Environment").run((context) -> {
			Compiler compiler = context.getBean(Compiler.class);
			assertThat(compiler.compile("Hello: {{nested.absent}}").execute(model)).isEqualTo("Hello: Environment");
		});
	}

	@MapAndObjectModelsTest
	void whenNestedPropertyIsInTheEnvironmentAndPartiallyNotInModelThenEnvironmentValueIsUsed(Object model) {
		this.contextRunner.withPropertyValues("nested.absent=Environment").run((context) -> {
			Compiler compiler = context.getBean(Compiler.class);
			assertThat(compiler.compile("Hello: {{nested.absent}}").execute(model)).isEqualTo("Hello: Environment");
		});
	}

	@MapAndObjectModelsTest
	void whenNestedPropertyIsInTheEnvironmentAndInModelThenModelValueIsUsed(Object model) {
		this.contextRunner.withPropertyValues("nested.name=Environment").run((context) -> {
			Compiler compiler = context.getBean(Compiler.class);
			assertThat(compiler.compile("Hello: {{nested.name}}").execute(model)).isEqualTo("Hello: Nested Model");
		});
	}

	@MapAndObjectModelsTest
	void whenTopLevelPropertyIsNotInTheEnvironmentOrTheModelThenCompilerDefaultIsUsed(Object model) {
		this.contextRunner.withUserConfiguration(CompilerDefaultValueConfiguration.class).run((context) -> {
			Compiler compiler = context.getBean(Compiler.class);
			assertThat(compiler.compile("Hello: {{absent}}").execute(model)).isEqualTo("Hello: Compiler Default");
		});
	}

	@MapAndObjectModelsTest
	void whenTopLevelPropertyIsNullInTheEnvironmentAndNotInTheModelThenCompilerNullValueIsUsed(Object model) {
		this.contextRunner
				.withInitializer((context) -> context.getEnvironment().getPropertySources()
						.addFirst(new MapPropertySource("test", Collections.singletonMap("absent", null))))
				.withUserConfiguration(CompilerNullValueConfiguration.class).run((context) -> {
					Compiler compiler = context.getBean(Compiler.class);
					assertThat(compiler.compile("Hello: {{absent}}").execute(model)).isEqualTo("Hello: Compiler Null");
				});
	}

	@MapAndObjectModelsTest
	void whenTopLevelPropertyIsNotInTheEnvironmentAndNullInTheModelThenCompilerNullValueIsUsed(Object model) {
		this.contextRunner.withUserConfiguration(CompilerNullValueConfiguration.class).run((context) -> {
			Compiler compiler = context.getBean(Compiler.class);
			assertThat(compiler.compile("Hello: {{none}}").execute(model)).isEqualTo("Hello: Compiler Null");
		});
	}

	@MapAndObjectModelsTest
	// TODO Should null in the model immediately use compiler's default for null or check
	// environment first? The test currently asserts that the environment is checked first
	void whenTopLevelPropertyIsInTheEnvironmentAndNullInTheModelThenEnvironmentValueIsUsed(Object model) {
		this.contextRunner.withPropertyValues("none=Environment").run((context) -> {
			Compiler compiler = context.getBean(Compiler.class);
			assertThat(compiler.compile("Hello: {{none}}").execute(model)).isEqualTo("Hello: Environment");
		});
	}

	@MapAndObjectModelsTest
	void whenNestedPropertyIsNotInTheEnvironmentOrTheModelThenCompilerDefaultIsUsed(Object model) {
		this.contextRunner.withUserConfiguration(CompilerDefaultValueConfiguration.class).run((context) -> {
			Compiler compiler = context.getBean(Compiler.class);
			assertThat(compiler.compile("Hello: {{nested.absent}}").execute(model))
					.isEqualTo("Hello: Compiler Default");
		});
	}

	@MapAndObjectModelsTest
	void whenNestedPropertyIsNullInTheEnvironmentAndNotInTheModelThenCompilerNullValueIsUsed(Object model) {
		this.contextRunner.withUserConfiguration(CompilerNullValueConfiguration.class)
				.withInitializer((context) -> context.getEnvironment().getPropertySources()
						.addFirst(new MapPropertySource("test", Collections.singletonMap("nested.absent", null))))
				.run((context) -> {
					Compiler compiler = context.getBean(Compiler.class);
					assertThat(compiler.compile("Hello: {{nested.absent}}").execute(model))
							.isEqualTo("Hello: Compiler Null");
				});
	}

	@MapAndObjectModelsTest
	void whenNestedPropertyIsNotInTheEnvironmentAndNullInTheModelThenCompilerNullValueIsUsed(Object model) {
		this.contextRunner.withUserConfiguration(CompilerNullValueConfiguration.class).run((context) -> {
			Compiler compiler = context.getBean(Compiler.class);
			assertThat(compiler.compile("Hello: {{nested.none}}").execute(model)).isEqualTo("Hello: Compiler Null");
		});
	}

	@MapAndObjectModelsTest
	// TODO Should null in the model immediately use compiler's default for null or check
	// environment first? The test currently asserts that the environment is checked first
	void whenNestedPropertyIsInTheEnvironmentAndNullInTheModelThenEnvironmentValueIsUsed(Object model) {
		this.contextRunner.withPropertyValues("nested.none=Environment").run((context) -> {
			Compiler compiler = context.getBean(Compiler.class);
			assertThat(compiler.compile("Hello: {{nested.none}}").execute(model)).isEqualTo("Hello: Environment");
		});
	}

	static Stream<Arguments> models() {
		Map<String, Object> mapModel = new HashMap<>();
		mapModel.put("name", "Model");
		mapModel.put("none", null);
		Map<String, Object> nestedMapModel = new HashMap<>();
		nestedMapModel.put("name", "Nested Model");
		nestedMapModel.put("none", null);
		mapModel.put("nested", nestedMapModel);
		return Stream.of(Arguments.of(new Model()), Arguments.of(mapModel));
	}

	static class Model {

		public String getName() {
			return "Model";
		}

		public Nested getNested() {
			return new Nested();
		}

		public String getNone() {
			return null;
		}

	}

	static class Nested {

		public String getName() {
			return "Nested Model";
		}

		public String getNone() {
			return null;
		}

	}

	@Configuration(proxyBeanMethods = false)
	static class CompilerDefaultValueConfiguration {

		@Bean
		static BeanPostProcessor compilerCustomizer() {
			return new BeanPostProcessor() {

				@Override
				public Object postProcessAfterInitialization(Object bean, String beanName) throws BeansException {
					if (!(bean instanceof Compiler)) {
						return bean;
					}
					return ((Compiler) bean).defaultValue("Compiler Default");
				}

			};
		}

	}

	@Configuration(proxyBeanMethods = false)
	static class CompilerNullValueConfiguration {

		@Bean
		static BeanPostProcessor compilerCustomizer() {
			return new BeanPostProcessor() {

				@Override
				public Object postProcessAfterInitialization(Object bean, String beanName) throws BeansException {
					if (!(bean instanceof Compiler)) {
						return bean;
					}
					return ((Compiler) bean).nullValue("Compiler Null");
				}

			};
		}

	}

	@Target(ElementType.METHOD)
	@Retention(RetentionPolicy.RUNTIME)
	@ParameterizedTest
	@MethodSource(value = "models")
	@interface MapAndObjectModelsTest {

	}

}
