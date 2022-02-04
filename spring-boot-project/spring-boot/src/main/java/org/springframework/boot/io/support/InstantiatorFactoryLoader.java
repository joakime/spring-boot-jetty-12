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

import java.util.List;

import org.springframework.boot.io.support.FactoriesLoader.FactoryArguments;
import org.springframework.boot.util.Instantiator;

/**
 * A reflective {@link FactoryLoader} that uses an {@link Instantiator} to load its
 * factory.
 *
 * @param <F> type of the factory
 * @author Andy Wilkinson
 */
class InstantiatorFactoryLoader<F> implements FactoryLoader<F> {

	private final Class<F> type;

	private final String id;

	@SuppressWarnings("unchecked")
	InstantiatorFactoryLoader(Class<?> type, String id) {
		this.type = (Class<F>) type;
		this.id = id;
	}

	@Override
	public Class<F> type() {
		return this.type;
	}

	@Override
	public String id() {
		return this.id;
	}

	@Override
	public F load(FactoryArguments args) {
		return new Instantiator<F>(this.type, (parameters) -> args.getArguments().forEach(parameters::add))
				.instantiate(List.of(this.id)).get(0);
	}

}
