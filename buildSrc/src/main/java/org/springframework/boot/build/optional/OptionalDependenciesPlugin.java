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

package org.springframework.boot.build.optional;

import java.util.HashMap;
import java.util.Map;

import groovy.util.Node;
import groovy.xml.QName;
import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.gradle.api.artifacts.Configuration;
import org.gradle.api.artifacts.ModuleDependency;
import org.gradle.api.artifacts.ModuleVersionIdentifier;
import org.gradle.api.artifacts.result.ResolvedDependencyResult;
import org.gradle.api.plugins.JavaPlugin;
import org.gradle.api.plugins.JavaPluginConvention;
import org.gradle.api.publish.PublishingExtension;
import org.gradle.api.publish.maven.MavenPublication;
import org.gradle.api.tasks.SourceSetContainer;
import org.gradle.plugins.ide.eclipse.EclipsePlugin;
import org.gradle.plugins.ide.eclipse.model.EclipseModel;

/**
 * A {@code Plugin} that adds support for Maven-style optional dependencies. Creates a new
 * {@code optional} configuration. The {@code optional} configuration is part of the
 * project's compile and runtime classpath's but does not affect the classpath of
 * dependent projects.
 *
 * @author Andy Wilkinson
 */
public class OptionalDependenciesPlugin implements Plugin<Project> {

	/**
	 * Name of the {@code optional} configuration.
	 */
	public static final String OPTIONAL_CONFIGURATION_NAME = "optional";

	@Override
	public void apply(Project project) {
		Configuration optional = project.getConfigurations().create("optional");
		project.getPlugins().withType(JavaPlugin.class, (javaPlugin) -> {
			SourceSetContainer sourceSets = project.getConvention().getPlugin(JavaPluginConvention.class)
					.getSourceSets();
			sourceSets.all((sourceSet) -> {
				sourceSet.setCompileClasspath(sourceSet.getCompileClasspath().plus(optional));
				sourceSet.setRuntimeClasspath(sourceSet.getRuntimeClasspath().plus(optional));
			});
		});
		project.getPlugins().withType(EclipsePlugin.class,
				(eclipePlugin) -> project.getExtensions().getByType(EclipseModel.class)
						.classpath((classpath) -> classpath.getPlusConfigurations().add(optional)));
		project.afterEvaluate((evaluatedProject) -> customizePublishedPom(evaluatedProject, optional));
	}

	private void customizePublishedPom(Project project, Configuration optional) {
		PublishingExtension publishing = project.getExtensions().findByType(PublishingExtension.class);
		if (publishing != null) {
			publishing.getPublications().withType(MavenPublication.class, (mavenPublication) -> {
				mavenPublication.getPom().withXml((xml) -> {
					Node dependencies = findChild(xml.asNode(), "dependencies");
					Map<String, String> versions = new HashMap<>();
					optional.getIncoming().getResolutionResult().getAllDependencies().forEach((dependencyResult) -> {
						if (dependencyResult instanceof ResolvedDependencyResult) {
							ModuleVersionIdentifier moduleVersion = ((ResolvedDependencyResult) dependencyResult)
									.getSelected().getModuleVersion();
							String key = moduleVersion.getGroup() + ":" + moduleVersion.getName();
							String version = moduleVersion.getVersion();
							versions.put(key, version);
						}
					});
					optional.getIncoming().getDependencies().withType(ModuleDependency.class)
							.forEach((moduleDependency) -> {
								Node dependency = new Node(dependencies, "dependency");
								new Node(dependency, "groupId").setValue(moduleDependency.getGroup());
								new Node(dependency, "artifactId").setValue(moduleDependency.getName());
								String id = moduleDependency.getGroup() + ":" + moduleDependency.getName();
								String version = versions.get(id);
								if (version == null) {
									System.out.println(id);
									System.out.println(versions);
									System.out.println(version);
								}
								new Node(dependency, "version").setValue(version);
								new Node(dependency, "optional").setValue(true);
							});
				});
			});
		}
	}

	private Node findChild(Node parent, String name) {
		for (Object child : parent.children()) {
			if (child instanceof Node) {
				Node node = (Node) child;
				if ((node.name() instanceof QName) && name.equals(((QName) node.name()).getLocalPart())) {
					return node;
				}
				if (name.equals(node.name())) {
					return node;
				}
			}
		}
		return null;
	}

}
