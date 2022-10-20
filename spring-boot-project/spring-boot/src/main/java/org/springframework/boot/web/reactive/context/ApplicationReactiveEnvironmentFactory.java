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

package org.springframework.boot.web.reactive.context;

import java.util.function.Supplier;

import org.springframework.boot.EnvironmentFactory;
import org.springframework.boot.WebApplicationType;
import org.springframework.core.env.StandardEnvironment;

/**
 * {@link EnvironmentFactory} for {@link ApplicationReactiveWebEnvironment}.
 *
 * @author Andy Wilkinson
 */
class ApplicationReactiveEnvironmentFactory implements EnvironmentFactory {

	@Override
	public boolean supports(WebApplicationType webApplicationType) {
		return WebApplicationType.REACTIVE == webApplicationType;
	}

	@Override
	public Class<? extends StandardEnvironment> environmentType() {
		return ApplicationReactiveWebEnvironment.class;
	}

	@Override
	public Supplier<StandardEnvironment> create() {
		return ApplicationReactiveWebEnvironment::new;
	}

}
