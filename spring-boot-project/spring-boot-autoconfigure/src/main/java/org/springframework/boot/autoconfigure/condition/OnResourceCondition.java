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

import org.springframework.context.annotation.ConditionContext;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.core.type.AnnotatedTypeMetadata;
import org.springframework.util.Assert;
import org.springframework.util.MultiValueMap;

/**
 * Adapter that enables annotation-based usage of {@link OnResourceRegistrationPredicate}.
 *
 * @author Dave Syer
 * @see ConditionalOnResource
 */
@Order(Ordered.HIGHEST_PRECEDENCE + 20)
class OnResourceCondition extends RegistrationPredicateCondition {

	@Override
	public boolean evaluate(ConditionContext context, AnnotatedTypeMetadata metadata) {
		MultiValueMap<String, Object> attributes = metadata
				.getAllAnnotationAttributes(ConditionalOnResource.class.getName(), true);
		List<String> locations = new ArrayList<>();
		collectValues(locations, attributes.get("resources"));
		Assert.isTrue(!locations.isEmpty(),
				"@ConditionalOnResource annotations must specify at least one resource location");
		return new OnResourceRegistrationPredicate(getLocation(metadata), locations)
				.test(new ConditionContextRegistrationContext(context));
	}

	private void collectValues(List<String> names, List<Object> values) {
		for (Object value : values) {
			for (Object item : (Object[]) value) {
				names.add((String) item);
			}
		}
	}

}
