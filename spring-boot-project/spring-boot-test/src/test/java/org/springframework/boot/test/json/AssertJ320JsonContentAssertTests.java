/*
 * Copyright 2012-2021 the original author or authors.
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

package org.springframework.boot.test.json;

import org.assertj.core.api.AbstractListAssert;
import org.assertj.core.api.AbstractMapAssert;
import org.junit.jupiter.api.Test;

import org.springframework.boot.testsupport.classpath.ClassPathOverrides;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.entry;

/**
 * Tests for {@link JsonContentAssert} with AssertJ 3.20.
 *
 * @author Andy Wilkinson
 */
@ClassPathOverrides("org.assertj:assertj-core:3.20.1")
class AssertJ320JsonContentAssertTests extends JsonContentAssertTests {

	@Test
	@Override
	void extractingJsonPathArrayValue() {
		// Override of containsExactly has been removed from ListAssert so we have to cast
		// to the superclass to allow code that compiles against < 3.20 to work with 3.20
		((AbstractListAssert<?, ?, Object, ?>) assertThat(forJson(TYPES)).extractingJsonPathArrayValue("@.arr"))
				.containsExactly(42);
	}

	@Test
	@Override
	@SuppressWarnings("unchecked")
	void extractingJsonPathMapValue() {
		// Override of contains has been removed from MapAssert so we have to cast to the
		// superclass to allow code that compiles against < 3.20 to work with 3.20
		((AbstractMapAssert<?, ?, Object, Object>) assertThat(forJson(TYPES)).extractingJsonPathMapValue("@.colorMap"))
				.contains(entry("red", "rojo"));
	}

}
