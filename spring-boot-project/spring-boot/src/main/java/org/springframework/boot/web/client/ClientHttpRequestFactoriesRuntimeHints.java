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

package org.springframework.boot.web.client;

import java.util.Objects;

import org.springframework.aot.hint.RuntimeHintsRegistrar;
import org.springframework.aot.hint.TypeReference;
import org.springframework.http.client.AbstractClientHttpRequestFactoryWrapper;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.http.client.OkHttp3ClientHttpRequestFactory;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.util.ClassUtils;
import org.springframework.util.ReflectionUtils;

/**
 * {@link RuntimeHintsRegistrar} for {@link ClientHttpRequestFactories}.
 *
 * @author Andy Wilkinson
 * @since 3.0.0
 */
public class ClientHttpRequestFactoriesRuntimeHints implements RuntimeHintsRegistrar {

	@Override
	public void registerHints(org.springframework.aot.hint.RuntimeHints hints, ClassLoader classLoader) {
		registerForClasspathDetection(hints, classLoader);
		registerForReflectiveConfiguration(hints, classLoader);
	}

	private static void registerForClasspathDetection(org.springframework.aot.hint.RuntimeHints hints,
			ClassLoader classLoader) {
		if (ClassUtils.isPresent(ClientHttpRequestFactories.APACHE_HTTP_CLIENT_CLASS, classLoader)) {
			hints.reflection().registerType(HttpComponentsClientHttpRequestFactory.class, (typeHint) -> typeHint
					.onReachableType(TypeReference.of(ClientHttpRequestFactories.APACHE_HTTP_CLIENT_CLASS)));
		}
		if (ClassUtils.isPresent(ClientHttpRequestFactories.OKHTTP_CLIENT_CLASS, classLoader)) {
			hints.reflection().registerType(OkHttp3ClientHttpRequestFactory.class, (typeHint) -> typeHint
					.onReachableType(TypeReference.of(ClientHttpRequestFactories.OKHTTP_CLIENT_CLASS)));
		}
		hints.reflection().registerType(SimpleClientHttpRequestFactory.class,
				(typeHint) -> typeHint.onReachableType(TypeReference.of(SimpleClientHttpRequestFactory.class)));
	}

	private static void registerForReflectiveConfiguration(org.springframework.aot.hint.RuntimeHints hints,
			ClassLoader classLoader) {
		hints.reflection().registerField(Objects.requireNonNull(
				ReflectionUtils.findField(AbstractClientHttpRequestFactoryWrapper.class, "requestFactory")));
		ClientHttpRequestFactories.Reflective.registerReflectionHints(hints.reflection());
	}

}
