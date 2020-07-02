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

import java.util.function.Supplier;

import org.springframework.context.annotation.ConditionContext;
import org.springframework.core.type.AnnotatedTypeMetadata;

/**
 * Adapter that enables annotation-based usage of
 * {@link OnPropertyListFunctionalCondition}.
 *
 * @author Eneias Silva
 * @author Stephane Nicoll
 * @since 2.0.5
 */
public class OnPropertyListCondition extends SpringBootCondition {

	private final String propertyName;

	private final Supplier<ConditionMessage.Builder> messageBuilder;

	/**
	 * Create a new instance with the property to check and the message builder to use.
	 * @param propertyName the name of the property
	 * @param messageBuilder a message builder supplier that should provide a fresh
	 * instance on each call
	 */
	protected OnPropertyListCondition(String propertyName, Supplier<ConditionMessage.Builder> messageBuilder) {
		this.propertyName = propertyName;
		this.messageBuilder = messageBuilder;
	}

	@Override
	public boolean matches(ConditionContext context, AnnotatedTypeMetadata metadata) {
		return new OnPropertyListFunctionalCondition(getLocation(metadata), this.propertyName, this.messageBuilder)
				.matches(context);
	}

}
