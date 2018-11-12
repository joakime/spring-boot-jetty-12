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

package org.springframework.boot.devtools.env;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.context.config.ConfigFileApplicationListener;
import org.springframework.boot.env.EnvironmentPostProcessor;
import org.springframework.core.annotation.AnnotationAwareOrderComparator;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MapPropertySource;
import org.springframework.core.env.StandardEnvironment;
import org.springframework.mock.env.MockEnvironment;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link DevToolsHomePropertiesPostProcessor}.
 *
 * @author Phillip Webb
 * @author Andy Wilkinson
 */
public class DevToolsHomePropertiesPostProcessorTests {

	@Rule
	public TemporaryFolder temp = new TemporaryFolder();

	private File home;

	@Before
	public void setup() throws IOException {
		this.home = this.temp.newFolder();
	}

	@Test
	public void loadsHomeProperties() throws Exception {
		writeHomeProperties(Collections.singletonMap("abc", "def"));
		ConfigurableEnvironment environment = new MockEnvironment();
		MockDevToolHomePropertiesPostProcessor postProcessor = new MockDevToolHomePropertiesPostProcessor();
		postProcessor.postProcessEnvironment(environment, null);
		assertThat(environment.getProperty("abc")).isEqualTo("def");
	}

	@Test
	public void ignoresMissingHomeProperties() {
		ConfigurableEnvironment environment = new MockEnvironment();
		MockDevToolHomePropertiesPostProcessor postProcessor = new MockDevToolHomePropertiesPostProcessor();
		postProcessor.postProcessEnvironment(environment, null);
		assertThat(environment.getProperty("abc")).isNull();
	}

	@Test
	public void profilesActivatedInHomePropertiesInfluenceFilesLoadedByConfigFileApplicationListener()
			throws IOException {
		ConfigurableEnvironment environment = new StandardEnvironment();
		environment.getPropertySources()
				.addLast(new MapPropertySource("custom-config-location",
						Collections.singletonMap("spring.config.location",
								this.temp.getRoot() + "/")));
		MockDevToolHomePropertiesPostProcessor homePropertiesPostProcessor = new MockDevToolHomePropertiesPostProcessor();
		ConfigFileApplicationListener configFilesPostProcessor = new ConfigFileApplicationListener();
		List<EnvironmentPostProcessor> postProcessors = Arrays
				.asList(homePropertiesPostProcessor, configFilesPostProcessor);
		AnnotationAwareOrderComparator.sort(postProcessors);
		writeHomeProperties(Collections.singletonMap("spring.profiles.active", "test"));
		writeProperties(Collections.singletonMap("some-prop", "general"),
				new File(this.temp.getRoot(), "application.properties"));
		writeProperties(Collections.singletonMap("some-prop", "profile-specific"),
				new File(this.temp.getRoot(), "application-test.properties"));
		SpringApplication application = new SpringApplication();
		postProcessors.forEach((postProcessor) -> postProcessor
				.postProcessEnvironment(environment, application));
		assertThat(environment.getProperty("some-prop")).isEqualTo("profile-specific");
	}

	private void writeHomeProperties(Map<String, String> map) throws IOException {
		writeProperties(map, new File(this.home, ".spring-boot-devtools.properties"));
	}

	private void writeProperties(Map<String, String> map, File file) throws IOException {
		Properties properties = new Properties();
		map.forEach(properties::setProperty);
		try (OutputStream out = new FileOutputStream(file)) {
			properties.store(out, null);
		}
	}

	private class MockDevToolHomePropertiesPostProcessor
			extends DevToolsHomePropertiesPostProcessor {

		@Override
		protected File getHomeFolder() {
			return DevToolsHomePropertiesPostProcessorTests.this.home;
		}

	}

}
