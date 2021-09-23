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

package org.springframework.boot.context.properties.source;

import java.util.Set;

import org.springframework.util.Assert;

/**
 * Exception thrown when more than one mutually exclusive configuration property has been
 * configured.
 *
 * @author Andy Wilkinson
 * @since 2.6.0
 */
@SuppressWarnings("serial")
public class MutuallyExclusiveConfigurationPropertiesException extends RuntimeException {

	private final Set<String> configuredNames;

	private final Set<String> mutuallyExclusiveNames;

	/**
	 * Creates a new instance for mutually exclusive configuration properties when two or
	 * more of those properties have been configured.
	 * @param mutuallyExclusiveNames the names of the properties that are mutually
	 * exclusive
	 * @param configuredNames the names of the properties that have been configured
	 */
	public MutuallyExclusiveConfigurationPropertiesException(Set<String> mutuallyExclusiveNames,
			Set<String> configuredNames) {
		super("The configuration properties '" + String.join(", ", mutuallyExclusiveNames)
				+ "' are mutually exclusive and '" + String.join(", ", configuredNames)
				+ "' have been configured together");
		Assert.isTrue(configuredNames != null && configuredNames.size() > 1,
				"configuredNames must contain 2 or more names");
		Assert.isTrue(mutuallyExclusiveNames != null && mutuallyExclusiveNames.size() > 1,
				"mutuallyExclusiveNames must contain 2 or more names");
		this.configuredNames = configuredNames;
		this.mutuallyExclusiveNames = mutuallyExclusiveNames;
	}

	/**
	 * Return the names of the properties that have been configured
	 * @return the names of the configured properties
	 */
	public Set<String> getConfiguredNames() {
		return this.configuredNames;
	}

	/**
	 * Return the names of the properties that are mutually exclusive
	 * @return the names of the mutually exclusive properties
	 */
	public Set<String> getMutuallyExclusiveNames() {
		return this.mutuallyExclusiveNames;
	}

}
