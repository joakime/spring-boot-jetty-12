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

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

/**
 * @author awilkinson
 */
public class OnClassRegistrationPredicate extends SpringBootRegistrationPredicate {

	private final List<Supplier<Class<?>>> classSuppliers = new ArrayList<>();

	public OnClassRegistrationPredicate(String location, Supplier<Class<?>> clazz) {
		super(location);
		this.classSuppliers.add(clazz);
	}

	@Override
	public ConditionOutcome getMatchOutcome(RegistrationContext context) {
		List<String> classes = new ArrayList<>();
		for (Supplier<Class<?>> classSupplier : this.classSuppliers) {
			try {
				classes.add(classSupplier.get().getName());
			}
			catch (NoClassDefFoundError ex) {
				Throwable cause = ex.getCause();
				String className = (cause instanceof ClassNotFoundException)
						? className = ((ClassNotFoundException) cause).getMessage() : ex.getMessage().replace("/", ".");
				return ConditionOutcome
						.noMatch(ConditionMessage.forCondition(OnClassRegistrationPredicate.class.getSimpleName())
								.didNotFind("class").items(className));
			}
		}
		return ConditionOutcome.match(ConditionMessage.forCondition(OnClassRegistrationPredicate.class.getSimpleName())
				.found("class", "classes").items(classes));
	}

}
