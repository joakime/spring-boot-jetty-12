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

import org.springframework.boot.io.support.FactoriesLoader.FactoryArguments;
import org.springframework.boot.io.support.FactoriesLoaderTests.Example;

/**
 * @author awilkinson
 */
public class BrokenExampleFactoryLoader implements FactoryLoader<Example> {

	@Override
	public Class<Example> type() {
		return Example.class;
	}

	@Override
	public String id() {
		return Example.class.getName();
	}

	@Override
	public Example load(FactoryArguments args) {
		throw new IllegalStateException("Cannot create factory");
	}

}
