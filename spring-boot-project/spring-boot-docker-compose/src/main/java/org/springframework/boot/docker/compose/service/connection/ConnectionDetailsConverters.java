/*
 * Copyright 2012-2023 the original author or authors.
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

package org.springframework.boot.docker.compose.service.connection;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Stream;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.boot.autoconfigure.service.connection.ConnectionDetails;
import org.springframework.core.ResolvableType;
import org.springframework.core.io.support.SpringFactoriesLoader;
import org.springframework.core.io.support.SpringFactoriesLoader.FailureHandler;
import org.springframework.util.Assert;

/**
 * A registry of {@link ConnectionDetailsConverter} instances.
 *
 * @author Andy Wilkinson
 */
class ConnectionDetailsConverters {

	private static final Log logger = LogFactory.getLog(ConnectionDetailsConverters.class);

	private List<Registration<?, ?>> registrations = new ArrayList<>();

	public ConnectionDetailsConverters() {
		this(SpringFactoriesLoader.forDefaultResourceLocation(ConnectionDetailsConverter.class.getClassLoader()));
	}

	@SuppressWarnings({ "rawtypes", "unchecked" })
	ConnectionDetailsConverters(SpringFactoriesLoader loader) {
		List<ConnectionDetailsConverter> factories = loader.load(ConnectionDetailsConverter.class,
				FailureHandler.logging(logger));
		Stream<Registration<?, ?>> registrations = factories.stream().map(Registration::get);
		registrations.filter(Objects::nonNull).forEach(this.registrations::add);
	}

	/**
	 * Return a {@link Map} of {@link ConnectionDetails} interface type to
	 * {@link ConnectionDetails} instance created by the converters associated with the
	 * given input.
	 * @param <I> the type of the input
	 * @param input the input
	 * @return a list of {@link ConnectionDetails} instances.
	 */
	public <I extends ConnectionDetails> Map<Class<?>, ConnectionDetails> convert(I input) {
		List<Registration<I, ?>> registrations = getRegistrations(input);
		Map<Class<?>, ConnectionDetails> result = new LinkedHashMap<>();
		for (Registration<I, ?> registration : registrations) {
			ConnectionDetails connectionDetails = registration.converter().convert(input);
			if (connectionDetails != null) {
				Class<?> outputType = registration.outputType();
				ConnectionDetails previous = result.put(outputType, connectionDetails);
				Assert.state(previous == null,
						() -> "Duplicate connection details supplied for %s".formatted(outputType.getName()));
			}
		}
		return Map.copyOf(result);
	}

	@SuppressWarnings("unchecked")
	<I extends ConnectionDetails> List<Registration<I, ?>> getRegistrations(I input) {
		Class<I> inputType = (Class<I>) input.getClass();
		List<Registration<I, ?>> result = new ArrayList<>();
		for (Registration<?, ?> candidate : this.registrations) {
			if (candidate.inputType().isAssignableFrom(inputType)) {
				result.add((Registration<I, ?>) candidate);
			}
		}
		return List.copyOf(result);
	}

	/**
	 * A {@link ConnectionDetailsConverter} registration.
	 *
	 * @param <I> the type of the input
	 * @param <O> the type of the output
	 * @param inputType the input type
	 * @param outputType the output type
	 * @param converter the converter
	 */
	record Registration<I extends ConnectionDetails, O extends ConnectionDetails>(Class<I> inputType,
			Class<O> outputType, ConnectionDetailsConverter<I, O> converter) {

		@SuppressWarnings("unchecked")
		private static <I extends ConnectionDetails, O extends ConnectionDetails> Registration<I, O> get(
				ConnectionDetailsConverter<I, O> factory) {
			ResolvableType type = ResolvableType.forClass(ConnectionDetailsConverter.class, factory.getClass());
			if (!type.hasUnresolvableGenerics()) {
				Class<?>[] generics = type.resolveGenerics();
				return new Registration<>((Class<I>) generics[0], (Class<O>) generics[1], factory);
			}
			return null;
		}

	}

}
