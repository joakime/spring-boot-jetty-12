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

package org.springframework.boot.validation.beanvalidation;

import java.util.Collection;

import org.springframework.aop.ClassFilter;
import org.springframework.aop.Pointcut;
import org.springframework.aop.support.ComposablePointcut;
import org.springframework.aop.support.DefaultPointcutAdvisor;
import org.springframework.validation.beanvalidation.MethodValidationPostProcessor;

/**
 * Custom {@link MethodValidationPostProcessor} that applies
 * {@code MethodValidationExclusionFilter exclusion filters}.
 *
 * @author Andy Wilkinson
 * @since 2.4.0
 */
public class ExcludingMethodValidationPostProcessor extends MethodValidationPostProcessor {

	private final Collection<MethodValidationExclusionFilter> exclusionFilters;

	/**
	 * Creates a new {@code ExcludingMethodValidationPostProcessor} that will apply the
	 * given {@code exclusionFilters} when identifying beans that are eligible for method
	 * validation post-processing.
	 * @param exclusionFilters filters to apply
	 */
	public ExcludingMethodValidationPostProcessor(Collection<MethodValidationExclusionFilter> exclusionFilters) {
		this.exclusionFilters = exclusionFilters;
	}

	@Override
	public void afterPropertiesSet() {
		super.afterPropertiesSet();
		DefaultPointcutAdvisor pointcutAdvisor = (DefaultPointcutAdvisor) this.advisor;
		Pointcut pointcut = pointcutAdvisor.getPointcut();
		pointcutAdvisor.setPointcut(new ComposablePointcut(pointcut.getClassFilter(), pointcut.getMethodMatcher())
				.intersection(new ExclusionFiltersClassFilter(this.exclusionFilters)));
	}

	static class ExclusionFiltersClassFilter implements ClassFilter {

		private final Collection<MethodValidationExclusionFilter> exclusionFilters;

		ExclusionFiltersClassFilter(Collection<MethodValidationExclusionFilter> exclusionFilters) {
			this.exclusionFilters = exclusionFilters;
		}

		@Override
		public boolean matches(Class<?> clazz) {
			for (MethodValidationExclusionFilter exclusionFilter : this.exclusionFilters) {
				if (exclusionFilter.exclude(clazz)) {
					return false;
				}
			}
			return true;
		}

	}

}