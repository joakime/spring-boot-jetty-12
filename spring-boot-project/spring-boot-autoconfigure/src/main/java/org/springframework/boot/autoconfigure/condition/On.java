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

package org.springframework.boot.autoconfigure.condition;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.function.Supplier;

import org.springframework.boot.autoconfigure.condition.OnBeanRegistrationPredicate.Spec;
import org.springframework.boot.autoconfigure.condition.OnPropertyRegistrationPredicate.PropertySpec;

public final class On {

	private final String location;

	Predicate<RegistrationContext> predicate;

	public On(String location) {
		this.location = location;
	}

	public void property(Consumer<PropertySpec> configurer) {
		PropertySpec spec = new PropertySpec();
		configurer.accept(spec);
		addPredicate(new OnPropertyRegistrationPredicate(this.location, Arrays.asList(spec)));
	}

	public void type(Supplier<Class<?>> type) {
		addPredicate(new OnClassRegistrationPredicate(this.location, type));
	}

	public void missingType(Supplier<Class<?>> type) {
		addPredicate(new OnMissingClassRegistrationPredicate(this.location, type));
	}

	private void addPredicate(Predicate<RegistrationContext> addition) {
		if (this.predicate == null) {
			this.predicate = addition;
		}
		else {
			this.predicate = this.predicate.and(addition);
		}
	}

	public Predicate<RegistrationContext> getPredicate() {
		return this.predicate;
	}

	public void setPredicate(Predicate<RegistrationContext> predicate) {
		this.predicate = predicate;
	}

	public void missingBean(Consumer<MissingBeanSpec> specConfigurer) {
		MissingBeanSpec spec = new MissingBeanSpec();
		specConfigurer.accept(spec);
		addPredicate(new OnBeanRegistrationPredicate(this.location, null, null,
				new Spec(Collections.emptySet(), spec.types, Collections.emptySet(), Collections.emptySet(),
						Collections.emptySet(), spec.searchStrategy, "MissingBean")));
	}

	public static class MissingBeanSpec {

		private Set<String> types = new HashSet<>();

		private SearchStrategy searchStrategy;

		public MissingBeanSpec searchStrategy(SearchStrategy searchStrategy) {
			this.searchStrategy = searchStrategy;
			return this;
		}

		public MissingBeanSpec type(Class<?> type) {
			this.types.add(type.getName());
			return this;
		}

	}

}