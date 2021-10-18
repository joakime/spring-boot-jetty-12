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

package org.springframework.boot.test.mock.mockito.hierarchy;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mockito;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.ApplicationContext;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.ContextHierarchy;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import static org.assertj.core.api.Assertions.assertThat;

@ContextHierarchy({ @ContextConfiguration(classes = ErrorIfContextReloaded.class, name = "parent"),
		@ContextConfiguration(classes = SomeService.class, name = "child"), })
@ExtendWith(SpringExtension.class)
class SomeTests {

	@Autowired
	ErrorIfContextReloaded a;

	@MockBean(context = "child")
	SomeService service;

	@Autowired
	ApplicationContext context;

	@Test
	void whenMockBeanSpecifiesContextThenOnlyTargettedContextHasMock() {
		assertThat(this.context.getParent().getBeansOfType(SomeService.class)).isEmpty();
		assertThat(Mockito.mockingDetails(this.service).isMock()).isTrue();
	}

}
