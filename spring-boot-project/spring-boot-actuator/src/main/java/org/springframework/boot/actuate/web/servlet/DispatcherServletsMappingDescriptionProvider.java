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

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.springframework.boot.actuate.web.MappingDescriptionProvider;
import org.springframework.data.rest.webmvc.support.DelegatingHandlerMapping;
import org.springframework.util.ClassUtils;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.DispatcherServlet;
import org.springframework.web.servlet.HandlerMapping;
import org.springframework.web.servlet.handler.AbstractUrlHandlerMapping;
import org.springframework.web.servlet.mvc.method.RequestMappingInfo;
import org.springframework.web.servlet.mvc.method.RequestMappingInfoHandlerMapping;

/**
 * A {@link MappingDescriptionProvider} that introspects the {@link HandlerMapping
 * HandlerMappings} that are known one or more {@link DispatcherServlet
 * DispatcherServlets}.
 *
 * @author Andy Wilkinson
 * @since 2.0.0
 */
public class DispatcherServletsMappingDescriptionProvider
		implements MappingDescriptionProvider {

	private static final List<HandlerMappingDescriptionProvider<? extends HandlerMapping>> descriptionProviders;

	static {
		List<HandlerMappingDescriptionProvider<? extends HandlerMapping>> providers = new ArrayList<>();
		providers.add(new RequestMappingInfoHandlerMappingDescriptionProvider());
		providers.add(new UrlHandlerMappingDescriptionProvider());
		if (ClassUtils.isPresent(
				"org.springframework.data.rest.webmvc.support.DelegatingHandlerMapping",
				null)) {
			providers.add(new DelegatingHandlerMappingDescriptionProvider(
					new ArrayList<>(providers)));
		}
		descriptionProviders = Collections.unmodifiableList(providers);
	}

	private final Map<String, DispatcherServlet> dispatcherServlets;

	/**
	 * Creates a new {@code DispatcherServletMappingDescriptionProvider} that will
	 * describe the mappings known to the given {@code dispatcherServlets}.
	 * @param dispatcherServlets the dispatcher servlets
	 */
	public DispatcherServletsMappingDescriptionProvider(
			Map<String, DispatcherServlet> dispatcherServlets) {
		this.dispatcherServlets = dispatcherServlets;
	}

	@Override
	public String getMappingName() {
		return "dispatcherServlets";
	}

	@Override
	public Map<String, List<DispatcherServletMappingDescription>> describeMappings() {
		Map<String, List<DispatcherServletMappingDescription>> mappings = new HashMap<>();
		for (Entry<String, DispatcherServlet> entry : this.dispatcherServlets
				.entrySet()) {
			mappings.put(entry.getKey(), describeMappings(entry.getValue()));
		}
		return mappings;
	}

	private List<DispatcherServletMappingDescription> describeMappings(
			DispatcherServlet dispatcherServlet) {
		List<HandlerMapping> handlerMappings = dispatcherServlet.getHandlerMappings();
		if (handlerMappings == null) {
			return Collections.emptyList();
		}
		return handlerMappings.stream().flatMap(this::describe)
				.collect(Collectors.toList());
	}

	private <T extends HandlerMapping> Stream<DispatcherServletMappingDescription> describe(
			T handlerMapping) {
		return describe(handlerMapping, descriptionProviders).stream();
	}

	@SuppressWarnings("unchecked")
	private static <T extends HandlerMapping> List<DispatcherServletMappingDescription> describe(
			T handlerMapping,
			List<HandlerMappingDescriptionProvider<?>> descriptionProviders) {
		for (HandlerMappingDescriptionProvider<?> descriptionProvider : descriptionProviders) {
			if (descriptionProvider.getMappingClass().isInstance(handlerMapping)) {
				return ((HandlerMappingDescriptionProvider<T>) descriptionProvider)
						.describe(handlerMapping);
			}
		}
		return Collections.emptyList();
	}

	private interface HandlerMappingDescriptionProvider<T extends HandlerMapping> {

		Class<T> getMappingClass();

		List<DispatcherServletMappingDescription> describe(T handlerMapping);

	}

	private static final class RequestMappingInfoHandlerMappingDescriptionProvider
			implements
			HandlerMappingDescriptionProvider<RequestMappingInfoHandlerMapping> {

		@Override
		public Class<RequestMappingInfoHandlerMapping> getMappingClass() {
			return RequestMappingInfoHandlerMapping.class;
		}

		@Override
		public List<DispatcherServletMappingDescription> describe(
				RequestMappingInfoHandlerMapping handlerMapping) {
			Map<RequestMappingInfo, HandlerMethod> handlerMethods = handlerMapping
					.getHandlerMethods();
			return handlerMethods.entrySet().stream().map(this::describe)
					.collect(Collectors.toList());
		}

		private DispatcherServletMappingDescription describe(
				Entry<RequestMappingInfo, HandlerMethod> mapping) {
			return new DispatcherServletMappingDescription(mapping.getKey().toString(),
					mapping.getValue().toString());
		}

	}

	private static final class UrlHandlerMappingDescriptionProvider
			implements HandlerMappingDescriptionProvider<AbstractUrlHandlerMapping> {

		@Override
		public Class<AbstractUrlHandlerMapping> getMappingClass() {
			return AbstractUrlHandlerMapping.class;
		}

		@Override
		public List<DispatcherServletMappingDescription> describe(
				AbstractUrlHandlerMapping handlerMapping) {
			return handlerMapping.getHandlerMap().entrySet().stream().map(this::describe)
					.collect(Collectors.toList());
		}

		private DispatcherServletMappingDescription describe(
				Entry<String, Object> mapping) {
			return new DispatcherServletMappingDescription(mapping.getKey().toString(),
					mapping.getValue().toString());
		}

	}

	private static final class DelegatingHandlerMappingDescriptionProvider
			implements HandlerMappingDescriptionProvider<DelegatingHandlerMapping> {

		private final List<HandlerMappingDescriptionProvider<?>> descriptionProviders;

		public DelegatingHandlerMappingDescriptionProvider(
				List<HandlerMappingDescriptionProvider<?>> descriptionProviders) {
			this.descriptionProviders = descriptionProviders;
		}

		@Override
		public Class<DelegatingHandlerMapping> getMappingClass() {
			return DelegatingHandlerMapping.class;
		}

		@Override
		public List<DispatcherServletMappingDescription> describe(
				DelegatingHandlerMapping handlerMapping) {
			List<DispatcherServletMappingDescription> descriptions = new ArrayList<>();
			for (HandlerMapping delegate : handlerMapping.getDelegates()) {
				descriptions.addAll(DispatcherServletsMappingDescriptionProvider
						.describe(delegate, this.descriptionProviders));
			}
			return descriptions;
		}

	}

}
