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

package org.springframework.boot.context.properties;

import org.junit.jupiter.api.Test;

import org.springframework.boot.validation.beanvalidation.MethodValidationExclusionFilter;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link ConfigurationPropertiesMethodValidationExclusionFilter}.
 *
 * @author Andy Wilkinson
 */
class ConfigurationPropertiesMethodValidationExclusionFilterTests {

	private final MethodValidationExclusionFilter filter = new ConfigurationPropertiesMethodValidationExclusionFilter();

	@Test
	void whenClassIsAnnotatedWithConfigurationPropertiesThenItIsExcluded() {
		assertThat(this.filter.exclude(Annotated.class)).isTrue();
	}

	@Test
	void whenClassIsNotAnnotatedWithConfigurationPropertiesThenItIsNotExcluded() {
		assertThat(this.filter.exclude(Plain.class)).isFalse();
	}

	static class Plain {

	}

	@ConfigurationProperties
	static class Annotated {

	}

}
