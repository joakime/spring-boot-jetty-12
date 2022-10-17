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

package org.springframework.boot.gradle.tasks.bundling;

import java.io.File;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Consumer;

import org.gradle.api.Project;
import org.gradle.api.artifacts.component.ComponentIdentifier;
import org.gradle.api.artifacts.component.ModuleComponentIdentifier;
import org.gradle.api.artifacts.component.ProjectComponentIdentifier;
import org.gradle.api.artifacts.result.ResolvedArtifactResult;
import org.gradle.api.provider.Provider;
import org.gradle.api.provider.SetProperty;

import org.springframework.boot.loader.tools.LibraryCoordinates;

/**
 * Tracks resolvable dependencies so that we can map from a file to the
 * {@link LibraryCoordinates} of the dependency that was resolved to that file.
 *
 * @author Andy Wilkinson
 */
class ResolvableDependencies {

	private final SetProperty<DependencyDescriptor> dependencyDescriptors;

	private final Provider<Map<String, ProjectDescriptor>> projectDescriptors;

	ResolvableDependencies(Project project) {
		this.dependencyDescriptors = project.getObjects().setProperty(DependencyDescriptor.class);
		this.projectDescriptors = project.provider(() -> {
			Map<String, ProjectDescriptor> projectDescriptors = new HashMap<>();
			for (Project p : project.getRootProject().getAllprojects()) {
				projectDescriptors.put(p.getPath(), new ProjectDescriptor(p));
			}
			return projectDescriptors;
		});
	}

	void resolvedArtifacts(Provider<Set<ResolvedArtifactResult>> resolvedArtifacts) {
		this.dependencyDescriptors
				.addAll(resolvedArtifacts.zip(this.projectDescriptors, this::asDependencyDescriptors));
	}

	private Set<DependencyDescriptor> asDependencyDescriptors(Set<ResolvedArtifactResult> artifacts,
			Map<String, ProjectDescriptor> projectDescriptors) {
		Set<DependencyDescriptor> descriptors = new HashSet<>();
		for (ResolvedArtifactResult result : artifacts) {
			asCoordinates(result, projectDescriptors, (coordinates) -> {
				boolean projectDependency = coordinates instanceof ProjectComponentIdentifierLibraryCoordinates;
				descriptors.add(new DependencyDescriptor(coordinates, projectDependency, result.getFile()));
			});
		}
		return descriptors;
	}

	Map<File, DependencyDescriptor> resolve() {
		Map<File, DependencyDescriptor> dependencyDescriptorsByFile = new HashMap<>();
		for (DependencyDescriptor dependencyDescriptor : this.dependencyDescriptors.get()) {
			dependencyDescriptorsByFile.put(dependencyDescriptor.getFile(), dependencyDescriptor);
		}
		return dependencyDescriptorsByFile;
	}

	private void asCoordinates(ResolvedArtifactResult resolvedArtifact,
			Map<String, ProjectDescriptor> projectDescriptors, Consumer<LibraryCoordinates> consumer) {
		System.out.println(projectDescriptors);
		ComponentIdentifier componentIdentifier = resolvedArtifact.getId().getComponentIdentifier();
		if (componentIdentifier instanceof ModuleComponentIdentifier) {
			consumer.accept(
					new ModuleComponentIdentifierLibraryCoordinates((ModuleComponentIdentifier) componentIdentifier));
		}
		else if (componentIdentifier instanceof ProjectComponentIdentifier) {
			ProjectDescriptor projectDescriptor = projectDescriptors
					.get(((ProjectComponentIdentifier) componentIdentifier).getProjectPath());
			if (projectDescriptor != null) {
				consumer.accept(new ProjectComponentIdentifierLibraryCoordinates(projectDescriptor));
			}
		}
	}

	/**
	 * Adapts a {@link ModuleComponentIdentifier} to {@link LibraryCoordinates}.
	 */
	private static class ModuleComponentIdentifierLibraryCoordinates implements LibraryCoordinates {

		private final ModuleComponentIdentifier identifier;

		ModuleComponentIdentifierLibraryCoordinates(ModuleComponentIdentifier identifier) {
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

		@Override
		public String toString() {
			return this.identifier.toString();
		}

	}

	/**
	 * Adapts a {@link ProjectComponentIdentifier} to {@link LibraryCoordinates}.
	 */
	private static class ProjectComponentIdentifierLibraryCoordinates implements LibraryCoordinates {

		private final ProjectDescriptor identifier;

		ProjectComponentIdentifierLibraryCoordinates(ProjectDescriptor identifier) {
			this.identifier = identifier;
		}

		@Override
		public String getGroupId() {
			return this.identifier.getGroup();
		}

		@Override
		public String getArtifactId() {
			return this.identifier.getName();
		}

		@Override
		public String getVersion() {
			return this.identifier.getVersion();
		}

		@Override
		public String toString() {
			return this.identifier.toString();
		}

	}

	/**
	 * Describes a dependency.
	 */
	static final class DependencyDescriptor {

		private final LibraryCoordinates coordinates;

		private final boolean projectDependency;

		private final File file;

		DependencyDescriptor(LibraryCoordinates coordinates, boolean projectDependency, File file) {
			this.coordinates = coordinates;
			this.projectDependency = projectDependency;
			this.file = file;
		}

		LibraryCoordinates getCoordinates() {
			return this.coordinates;
		}

		boolean isProjectDependency() {
			return this.projectDependency;
		}

		private File getFile() {
			return this.file;
		}

		@Override
		public String toString() {
			return this.coordinates.toString();
		}

	}

	private static final class ProjectDescriptor {

		private final String group;

		private final String name;

		private final String version;

		ProjectDescriptor(Project project) {
			this.group = Objects.toString(project.getGroup());
			this.name = Objects.toString(project.getName());
			this.version = Objects.toString(project.getVersion());
		}

		String getGroup() {
			return this.group;
		}

		String getName() {
			return this.name;
		}

		String getVersion() {
			return this.version;
		}

		@Override
		public String toString() {
			return this.group + ":" + this.name + ":" + this.version;
		}

	}

}
