/*
 * Copyright 2012-2023 the original author or authors.
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

package org.springframework.boot.configurationmetadata.changelog;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.springframework.boot.configurationmetadata.ConfigurationMetadataProperty;
import org.springframework.boot.configurationmetadata.ConfigurationMetadataRepository;
import org.springframework.boot.configurationmetadata.Deprecation.Level;
import org.springframework.boot.configurationmetadata.changelog.ConfigurationMetadataDiff.Difference.Type;

/**
 * A diff of two repositories of configuration metadata.
 *
 * @author Stephane Nicoll
 * @author Andy Wilkinson
 */
final class ConfigurationMetadataDiff {

	private final String leftName;

	private final String rightName;

	private final List<Difference> differences;

	private ConfigurationMetadataDiff(NamedConfigurationMetadataRepository left,
			NamedConfigurationMetadataRepository right) {
		this.leftName = left.getName();
		this.rightName = right.getName();
		this.differences = differences(left, right);
	}

	String getLeftName() {
		return this.leftName;
	}

	String getRightName() {
		return this.rightName;
	}

	static ConfigurationMetadataDiff of(NamedConfigurationMetadataRepository left,
			NamedConfigurationMetadataRepository right) {
		return new ConfigurationMetadataDiff(left, right);
	}

	private static List<Difference> differences(ConfigurationMetadataRepository left,
			ConfigurationMetadataRepository right) {
		List<Difference> differences = new ArrayList<>();
		List<String> matches = new ArrayList<>();
		Map<String, ConfigurationMetadataProperty> leftProperties = left.getAllProperties();
		Map<String, ConfigurationMetadataProperty> rightProperties = right.getAllProperties();
		for (ConfigurationMetadataProperty leftProperty : leftProperties.values()) {
			String id = leftProperty.getId();
			matches.add(id);
			ConfigurationMetadataProperty rightProperty = rightProperties.get(id);
			if (rightProperty == null) {
				if (!(leftProperty.isDeprecated() && leftProperty.getDeprecation().getLevel() == Level.ERROR)) {
					differences.add(new Difference(Type.DELETED, leftProperty, null));
				}
			}
			else if (rightProperty.isDeprecated() && !leftProperty.isDeprecated()) {
				differences.add(new Difference(Type.DEPRECATED, leftProperty, rightProperty));
			}
			else if (leftProperty.isDeprecated() && leftProperty.getDeprecation().getLevel() == Level.WARNING
					&& rightProperty.isDeprecated() && rightProperty.getDeprecation().getLevel() == Level.ERROR) {
				differences.add(new Difference(Type.DELETED, leftProperty, rightProperty));
			}
		}
		for (ConfigurationMetadataProperty rightProperty : rightProperties.values()) {
			if ((!matches.contains(rightProperty.getId())) && (!rightProperty.isDeprecated())) {
				differences.add(new Difference(Type.ADDED, null, rightProperty));
			}
		}
		return differences;
	}

	List<Difference> getDifferences() {
		return this.differences;
	}

	/**
	 * A difference in the metadata.
	 */
	static final class Difference {

		private final Type type;

		private final ConfigurationMetadataProperty left;

		private final ConfigurationMetadataProperty right;

		private Difference(Type type, ConfigurationMetadataProperty left, ConfigurationMetadataProperty right) {
			this.type = type;
			this.left = left;
			this.right = right;
		}

		Type type() {
			return this.type;
		}

		ConfigurationMetadataProperty left() {
			return this.left;
		}

		ConfigurationMetadataProperty right() {
			return this.right;
		}

		/**
		 * The type of a difference in the metadata.
		 */
		enum Type {

			/**
			 * The entry has been added.
			 */
			ADDED,

			/**
			 * The entry has been made deprecated. It may or may not still exist in the
			 * previous version.
			 */
			DEPRECATED,

			/**
			 * The entry has been deleted.
			 */
			DELETED

		}

	}

}
