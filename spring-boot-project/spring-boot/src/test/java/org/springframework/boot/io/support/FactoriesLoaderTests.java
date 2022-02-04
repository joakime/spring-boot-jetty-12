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

package org.springframework.boot.io.support;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.List;

import org.junit.jupiter.api.Test;

import org.springframework.boot.io.support.FactoriesLoader.FactoryArguments;
import org.springframework.boot.io.support.FactoriesLoader.LoggingFailureHandler;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalStateException;

/**
 * @author awilkinson
 */
public class FactoriesLoaderTests {

	@Test
	public void serviceLoader() throws MalformedURLException {
		URLClassLoader classLoader = new URLClassLoader(new URL[] {
				new File("src/test/resources/org/springframework/boot/io/support/service-loader").toURI().toURL() },
				getClass().getClassLoader());
		FactoryArguments arguments = new FactoryArguments();
		arguments.add(String.class, "Hello, World");
		List<Example> examples = new FactoriesLoader(classLoader).load(Example.class, arguments);
		assertThat(examples).hasSize(2);
	}

	@Test
	public void springFactories() throws MalformedURLException {
		URLClassLoader classLoader = new URLClassLoader(new URL[] {
				new File("src/test/resources/org/springframework/boot/io/support/spring-factories").toURI().toURL() },
				getClass().getClassLoader());
		FactoryArguments arguments = new FactoryArguments();
		arguments.add(String.class, "Hello, World");
		List<Example> examples = new FactoriesLoader(classLoader).load(Example.class, arguments);
		assertThat(examples).hasSize(1);
	}

	@Test
	public void brokenFactoryCausesLoadFailureByDefault() throws MalformedURLException {
		URLClassLoader classLoader = new URLClassLoader(new URL[] {
				new File("src/test/resources/org/springframework/boot/io/support/broken-factory").toURI().toURL() },
				getClass().getClassLoader());
		assertThatIllegalStateException()
				.isThrownBy(() -> new FactoriesLoader(classLoader).load(Example.class, new FactoryArguments()));
	}

	@Test
	public void brokenFactoriesCanBeIgnored() throws MalformedURLException {
		URLClassLoader classLoader = new URLClassLoader(new URL[] {
				new File("src/test/resources/org/springframework/boot/io/support/broken-factory").toURI().toURL() },
				getClass().getClassLoader());
		List<Example> examples = new FactoriesLoader(classLoader).load(Example.class, new FactoryArguments(),
				new LoggingFailureHandler());
		assertThat(examples).hasSize(1);
	}

	interface Example {

	}

	static class DefaultExample implements Example {

	}

	static class ConstructorArgsExample implements Example {

		private final String text;

		ConstructorArgsExample(String text) {
			this.text = text;
		}

	}

}
