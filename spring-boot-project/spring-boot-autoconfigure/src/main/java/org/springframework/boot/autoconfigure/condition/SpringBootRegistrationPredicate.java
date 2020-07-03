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

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.util.ClassUtils;
import org.springframework.util.StringUtils;

/**
 * @author awilkinson
 */
public abstract class SpringBootRegistrationPredicate implements RegistrationPredicate {

	private final Log logger = LogFactory.getLog(getClass());

	private final String location;

	public SpringBootRegistrationPredicate(String location) {
		this.location = location;
	}

	@Override
	public boolean test(RegistrationContext context) {
		try {
			ConditionOutcome outcome = getMatchOutcome(context);
			logOutcome(outcome);
			recordEvaluation(context, outcome);
			return outcome.isMatch();
		}
		catch (RuntimeException ex) {
			throw new IllegalStateException("Error testing registration predicate at " + this.location, ex);
		}
	}

	/**
	 * Determine the outcome of the match along with suitable log output.
	 * @param context the registration context context
	 * @return the outcome
	 */
	public abstract ConditionOutcome getMatchOutcome(RegistrationContext context);

	protected final void logOutcome(ConditionOutcome outcome) {
		if (this.logger.isTraceEnabled()) {
			this.logger.trace(getLogMessage(outcome));
		}
	}

	private StringBuilder getLogMessage(ConditionOutcome outcome) {
		StringBuilder message = new StringBuilder();
		message.append("Condition ");
		message.append(ClassUtils.getShortName(getClass()));
		message.append(" on ");
		message.append(this.location);
		message.append(outcome.isMatch() ? " matched" : " did not match");
		if (StringUtils.hasLength(outcome.getMessage())) {
			message.append(" due to ");
			message.append(outcome.getMessage());
		}
		return message;
	}

	private void recordEvaluation(RegistrationContext context, ConditionOutcome outcome) {
		if (context.getBeanFactory() != null) {
			ConditionEvaluationReport.get(context.getBeanFactory()).recordConditionEvaluation(this.location, this,
					outcome);
		}
	}

	/**
	 * Slightly faster variant of {@link ClassUtils#forName(String, ClassLoader)} that
	 * doesn't deal with primitives, arrays or inner types.
	 * @param className the class name to resolve
	 * @param classLoader the class loader to use
	 * @return a resolved class
	 * @throws ClassNotFoundException if the class cannot be found
	 */
	protected static Class<?> resolve(String className, ClassLoader classLoader) throws ClassNotFoundException {
		if (classLoader != null) {
			return Class.forName(className, false, classLoader);
		}
		return Class.forName(className);
	}

}
