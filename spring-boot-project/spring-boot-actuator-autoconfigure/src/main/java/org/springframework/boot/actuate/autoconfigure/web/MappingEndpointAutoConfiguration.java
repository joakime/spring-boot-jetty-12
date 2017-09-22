/*
 * Copyright 2012-2017 the original author or authors.
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

package org.springframework.boot.actuate.autoconfigure.web;

import java.util.Collections;
import java.util.List;

import javax.servlet.ServletContext;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.actuate.autoconfigure.endpoint.condition.ConditionalOnEnabledEndpoint;
import org.springframework.boot.actuate.web.MappingDescriptionProvider;
import org.springframework.boot.actuate.web.MappingEndpoint;
import org.springframework.boot.actuate.web.reactive.DispatcherHandlersMappingDescriptionProvider;
import org.springframework.boot.actuate.web.servlet.DispatcherServletsMappingDescriptionProvider;
import org.springframework.boot.actuate.web.servlet.FiltersMappingDescriptionProvider;
import org.springframework.boot.actuate.web.servlet.ServletsMappingDescriptionProvider;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication.Type;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.DispatcherHandler;
import org.springframework.web.servlet.DispatcherServlet;

/**
 * {@link EnableAutoConfiguration Auto-configuration} for {@link MappingEndpoint}.
 *
 * @author Andy Wilkinson
 * @since 2.0.0
 */
@Configuration
@ConditionalOnWebApplication
public class MappingEndpointAutoConfiguration {

	@Bean
	@ConditionalOnEnabledEndpoint
	public MappingEndpoint mappingEndpoint(
			ObjectProvider<List<MappingDescriptionProvider>> mappingDescriptionProviders) {
		return new MappingEndpoint(
				mappingDescriptionProviders.getIfAvailable(Collections::emptyList));
	}

	@Configuration
	@ConditionalOnWebApplication(type = Type.SERVLET)
	static class ServletWebConfiguration {

		@Configuration
		@ConditionalOnClass(DispatcherServlet.class)
		@ConditionalOnBean(DispatcherServlet.class)
		static class SpringMvcConfiguration {

			@Bean
			DispatcherServletsMappingDescriptionProvider dispatcherServletMappingDescriptionProvider(
					ApplicationContext applicationContext) {
				return new DispatcherServletsMappingDescriptionProvider(
						applicationContext.getBeansOfType(DispatcherServlet.class));
			}

		}

		@Bean
		ServletsMappingDescriptionProvider servletMappingDescriptionProvider(
				ServletContext servletContext) {
			return new ServletsMappingDescriptionProvider(servletContext);
		}

		@Bean
		FiltersMappingDescriptionProvider filterMappingDescriptionProvider(
				ServletContext servletContext) {
			return new FiltersMappingDescriptionProvider(servletContext);
		}

	}

	@Configuration
	@ConditionalOnWebApplication(type = Type.REACTIVE)
	@ConditionalOnClass(DispatcherHandler.class)
	@ConditionalOnBean(DispatcherHandler.class)
	static class ReactiveWebConfiguration {

		@Bean
		public DispatcherHandlersMappingDescriptionProvider dispatcherHandlerMappingDescriptionProvider(
				ApplicationContext applicationContext) {
			return new DispatcherHandlersMappingDescriptionProvider(
					applicationContext.getBeansOfType(DispatcherHandler.class));
		}

	}

}
