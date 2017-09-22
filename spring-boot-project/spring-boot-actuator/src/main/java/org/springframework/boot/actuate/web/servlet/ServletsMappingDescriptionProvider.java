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

package org.springframework.boot.actuate.web.servlet;

import java.util.List;
import java.util.stream.Collectors;

import javax.servlet.Servlet;
import javax.servlet.ServletContext;

import org.springframework.boot.actuate.web.MappingDescriptionProvider;

/**
 * A {@link MappingDescriptionProvider} that describes that mappings of any {@link Servlet
 * Servlets} registered with a {@link ServletContext}.
 *
 * @author Andy Wilkinson
 * @since 2.0
 */
public class ServletsMappingDescriptionProvider implements MappingDescriptionProvider {

	private final ServletContext servletContext;

	/**
	 * Creates a new {@code ServletContextMappingDescriptionProvider} that will describe
	 * mappings by introspecting the given {@code servletContext}.
	 * @param servletContext the context
	 */
	public ServletsMappingDescriptionProvider(ServletContext servletContext) {
		this.servletContext = servletContext;
	}

	@Override
	public List<ServletRegistrationMappingDescription> describeMappings() {
		return this.servletContext.getServletRegistrations().values().stream()
				.map(ServletRegistrationMappingDescription::new)
				.collect(Collectors.toList());
	}

	@Override
	public String getMappingName() {
		return "servlets";
	}

}
