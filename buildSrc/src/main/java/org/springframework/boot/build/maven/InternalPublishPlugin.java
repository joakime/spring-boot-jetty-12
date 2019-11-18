/*
 * Copyright 2012-2019 the original author or authors.
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

package org.springframework.boot.build.maven;

import java.io.File;

import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.gradle.api.artifacts.Configuration;
import org.gradle.api.publish.PublishingExtension;

/**
 * {@link Plugin} to publish a project to an internal Maven repository for consumption by
 * Maven-based parts of the build.
 *
 * @author Andy Wilkinson
 */
public class InternalPublishPlugin implements Plugin<Project> {

	@Override
	public void apply(Project project) {
		PublishingExtension publishing = project.getExtensions().getByType(PublishingExtension.class);
		File repositoryLocation = new File(project.getBuildDir(), "internal-maven-repository");
		publishing.getRepositories().maven((mavenRepository) -> {
			mavenRepository.setName("internal");
			mavenRepository.setUrl(repositoryLocation.toURI());
		});
		Configuration mavenRepository = project.getConfigurations().create("mavenRepository");
		project.getArtifacts().add(mavenRepository.getName(), repositoryLocation,
				(artifact) -> artifact.builtBy("publishMavenPublicationToInternalRepository"));
	}

}
