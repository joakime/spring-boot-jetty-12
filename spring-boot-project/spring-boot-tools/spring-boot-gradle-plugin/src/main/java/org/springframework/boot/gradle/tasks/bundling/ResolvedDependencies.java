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

package org.springframework.boot.gradle.tasks.bundling;

import java.io.File;
import java.util.Set;

import org.gradle.api.artifacts.ResolvedConfiguration;
import org.gradle.api.artifacts.component.ComponentArtifactIdentifier;
import org.gradle.api.artifacts.component.ModuleComponentIdentifier;
import org.gradle.api.artifacts.result.ResolvedArtifactResult;
import org.gradle.internal.component.external.model.ModuleComponentArtifactIdentifier;

import org.springframework.boot.loader.tools.LibraryCoordinates;

/**
 * Provides details of resolved dependencies in the project so we can find
 * {@link LibraryCoordinates}.
 *
 * @author Madhura Bhave
 * @author Scott Frederick
 * @author Phillip Webb
 * @author Paddy Drury
 * @author Andy Wilkinson
 */
class ResolvedDependencies {

	private final Set<String> projectIds;

	private final Set<ResolvedArtifactResult> resolvedArtifactResults;

	ResolvedDependencies(Set<String> projectIds, Set<ResolvedArtifactResult> resolvedArtifactResults) {
		this.projectIds = projectIds;
		this.resolvedArtifactResults = resolvedArtifactResults;
	}

	DependencyDescriptor find(File file) {
		for (ResolvedArtifactResult result : this.resolvedArtifactResults) {
			if (result.getFile().equals(file)) {
				ComponentArtifactIdentifier id = result.getId();
				if (id instanceof ModuleComponentArtifactIdentifier) {
					ModuleComponentIdentifier moduleId = ((ModuleComponentArtifactIdentifier) id)
						.getComponentIdentifier();
					boolean projectDependency = this.projectIds
						.contains(moduleId.getGroup() + ":" + moduleId.getModule() + ":" + moduleId.getVersion());
					return new DependencyDescriptor(new ModuleComponentIdentifierLibraryCoordinates(moduleId),
							projectDependency);
				}
			}
		}
		return null;
	}

	private static final class ModuleComponentIdentifierLibraryCoordinates implements LibraryCoordinates {

		private final ModuleComponentIdentifier identifier;

		private ModuleComponentIdentifierLibraryCoordinates(ModuleComponentIdentifier identifier) {
			this.identifier = identifier;
		}

		@Override
		public String getGroupId() {
			return this.identifier.getGroup();
		}

		@Override
		public String getArtifactId() {
			return this.identifier.getModule();
		}

		@Override
		public String getVersion() {
			return this.identifier.getVersion();
		}

	}

	/**
	 * Describes a dependency in a {@link ResolvedConfiguration}.
	 */
	static final class DependencyDescriptor {

		private final LibraryCoordinates coordinates;

		private final boolean projectDependency;

		private DependencyDescriptor(LibraryCoordinates coordinates, boolean projectDependency) {
			this.coordinates = coordinates;
			this.projectDependency = projectDependency;
		}

		LibraryCoordinates getCoordinates() {
			return this.coordinates;
		}

		boolean isProjectDependency() {
			return this.projectDependency;
		}

	}

}
