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

package org.springframework.boot;

import java.io.PrintStream;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.MockitoAnnotations;

import org.springframework.boot.Banner.Mode;
import org.springframework.boot.testsupport.system.CapturedOutput;
import org.springframework.boot.testsupport.system.OutputCaptureExtension;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;

/**
 * Tests for {@link Banner} and its usage by {@link SpringApplication}.
 *
 * @author Phillip Webb
 * @author Michael Stummvoll
 * @author Michael Simons
 */
@ExtendWith(OutputCaptureExtension.class)
public class BannerTests {

	private ConfigurableApplicationContext context;

	@AfterEach
	public void cleanUp() {
		if (this.context != null) {
			this.context.close();
		}
	}

	@Captor
	private ArgumentCaptor<Class<?>> sourceClassCaptor;

	@BeforeEach
	public void setup() {
		MockitoAnnotations.initMocks(this);
	}

	@Test
	public void testDefaultBanner(CapturedOutput capturedOutput) {
		SpringApplication application = createSpringApplication();
		this.context = application.run();
		assertThat(capturedOutput).contains(":: Spring Boot ::");
	}

	@Test
	public void testDefaultBannerInLog(CapturedOutput capturedOutput) {
		SpringApplication application = createSpringApplication();
		this.context = application.run();
		assertThat(capturedOutput).contains(":: Spring Boot ::");
	}

	@Test
	public void testCustomBanner(CapturedOutput capturedOutput) {
		SpringApplication application = createSpringApplication();
		application.setBanner(new DummyBanner());
		this.context = application.run();
		assertThat(capturedOutput).contains("My Banner");
	}

	@Test
	public void testBannerInContext() {
		SpringApplication application = createSpringApplication();
		this.context = application.run();
		assertThat(this.context.containsBean("springBootBanner")).isTrue();
	}

	@Test
	public void testCustomBannerInContext() {
		SpringApplication application = createSpringApplication();
		Banner banner = mock(Banner.class);
		application.setBanner(banner);
		this.context = application.run();
		Banner printedBanner = (Banner) this.context.getBean("springBootBanner");
		assertThat(printedBanner).hasFieldOrPropertyWithValue("banner", banner);
		verify(banner).printBanner(any(Environment.class),
				this.sourceClassCaptor.capture(), any(PrintStream.class));
		reset(banner);
		printedBanner.printBanner(this.context.getEnvironment(), null, System.out);
		verify(banner).printBanner(any(Environment.class),
				eq(this.sourceClassCaptor.getValue()), any(PrintStream.class));
	}

	@Test
	public void testDisableBannerInContext() {
		SpringApplication application = createSpringApplication();
		application.setBannerMode(Mode.OFF);
		this.context = application.run();
		assertThat(this.context.containsBean("springBootBanner")).isFalse();
	}

	private SpringApplication createSpringApplication() {
		SpringApplication application = new SpringApplication(Config.class);
		application.setWebApplicationType(WebApplicationType.NONE);
		return application;
	}

	static class DummyBanner implements Banner {

		@Override
		public void printBanner(Environment environment, Class<?> sourceClass,
				PrintStream out) {
			out.println("My Banner");
		}

	}

	@Configuration(proxyBeanMethods = false)
	public static class Config {

	}

}
