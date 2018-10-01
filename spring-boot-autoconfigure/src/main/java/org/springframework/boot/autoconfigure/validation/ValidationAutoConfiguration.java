/*
 * Copyright 2012-2018 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.boot.autoconfigure.validation;

import java.util.Set;

import javax.validation.ConstraintViolation;
import javax.validation.Validator;
import javax.validation.executable.ExecutableValidator;
import javax.validation.metadata.BeanDescriptor;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnResource;
import org.springframework.boot.bind.RelaxedPropertyResolver;
import org.springframework.boot.validation.MessageInterpolatorFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Role;
import org.springframework.core.env.Environment;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;
import org.springframework.validation.beanvalidation.MethodValidationPostProcessor;

/**
 * {@link EnableAutoConfiguration Auto-configuration} to configure the validation
 * infrastructure.
 *
 * @author Stephane Nicoll
 * @since 1.5.0
 */
@Configuration
@ConditionalOnClass(ExecutableValidator.class)
@ConditionalOnResource(resources = "classpath:META-INF/services/javax.validation.spi.ValidationProvider")
@Import(PrimaryDefaultValidatorPostProcessor.class)
public class ValidationAutoConfiguration {

	@Bean
	@Role(BeanDefinition.ROLE_INFRASTRUCTURE)
	@ConditionalOnMissingBean(Validator.class)
	public static LocalValidatorFactoryBean defaultValidator() {
		LocalValidatorFactoryBean factoryBean = new LocalValidatorFactoryBean();
		MessageInterpolatorFactory interpolatorFactory = new MessageInterpolatorFactory();
		factoryBean.setMessageInterpolator(interpolatorFactory.getObject());
		return factoryBean;
	}

	@Bean
	@ConditionalOnMissingBean
	public static MethodValidationPostProcessor methodValidationPostProcessor(
			Environment environment, ObjectProvider<Validator> validatorProvider) {
		MethodValidationPostProcessor processor = new MethodValidationPostProcessor();
		processor.setProxyTargetClass(determineProxyTargetClass(environment));
		processor.setValidator(new LazyValidator(validatorProvider));
		return processor;
	}

	private static boolean determineProxyTargetClass(Environment environment) {
		RelaxedPropertyResolver resolver = new RelaxedPropertyResolver(environment,
				"spring.aop.");
		Boolean value = resolver.getProperty("proxyTargetClass", Boolean.class);
		return (value != null) ? value : true;
	}

	private static final class LazyValidator implements Validator {

		private final ObjectProvider<Validator> validatorProvider;

		private volatile Validator validator;

		LazyValidator(ObjectProvider<Validator> validatorProvider) {
			this.validatorProvider = validatorProvider;
			this.validator = this.validator;
		}

		@Override
		public <T> Set<ConstraintViolation<T>> validate(T object, Class<?>... groups) {
			return getValidator().validate(object, groups);
		}

		@Override
		public <T> Set<ConstraintViolation<T>> validateProperty(T object,
				String propertyName, Class<?>... groups) {
			return getValidator().validateProperty(object, propertyName, groups);
		}

		@Override
		public <T> Set<ConstraintViolation<T>> validateValue(Class<T> beanType,
				String propertyName, Object value, Class<?>... groups) {
			return getValidator().validateValue(beanType, propertyName, value, groups);
		}

		@Override
		public BeanDescriptor getConstraintsForClass(Class<?> clazz) {
			return getValidator().getConstraintsForClass(clazz);
		}

		@Override
		public <T> T unwrap(Class<T> type) {
			return getValidator().unwrap(type);
		}

		@Override
		public ExecutableValidator forExecutables() {
			return getValidator().forExecutables();
		}

		private Validator getValidator() {
			if (this.validator == null) {
				this.validator = this.validatorProvider.getObject();
			}
			return this.validator;
		}

	}

}
