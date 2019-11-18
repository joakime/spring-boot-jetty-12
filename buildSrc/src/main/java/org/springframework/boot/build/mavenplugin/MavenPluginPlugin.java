/*
 * Copyright 2019 the original author or authors.
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

package org.springframework.boot.build.mavenplugin;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.gradle.api.artifacts.Configuration;
import org.gradle.api.artifacts.ProjectDependency;
import org.gradle.api.component.SoftwareComponent;
import org.gradle.api.plugins.JavaLibraryPlugin;
import org.gradle.api.plugins.JavaPlugin;
import org.gradle.api.plugins.JavaPluginConvention;
import org.gradle.api.publish.PublishingExtension;
import org.gradle.api.publish.maven.MavenPublication;
import org.gradle.api.publish.maven.plugins.MavenPublishPlugin;
import org.gradle.api.tasks.Copy;
import org.gradle.api.tasks.JavaExec;
import org.gradle.api.tasks.SourceSet;
import org.gradle.api.tasks.SourceSetContainer;
import org.gradle.api.tasks.bundling.Jar;

import org.springframework.boot.build.maven.InternalPublishPlugin;

/**
 * Plugin for building Spring Boot's Maven Plugin.
 *
 * @author Andy Wilkinson
 */
public class MavenPluginPlugin implements Plugin<Project> {

	@Override
	public void apply(Project project) {
		project.getPlugins().apply(JavaLibraryPlugin.class);
		project.getPlugins().apply(MavenPublishPlugin.class);
		project.getPlugins().apply(InternalPublishPlugin.class);
		Configuration mavenRepository = project.getConfigurations().maybeCreate("mavenRepository");
		project.getConfigurations().getByName(JavaPlugin.IMPLEMENTATION_CONFIGURATION_NAME).getDependencies()
				.all((dependency) -> {
					if (dependency instanceof ProjectDependency) {
						ProjectDependency projectDependency = (ProjectDependency) dependency;
						Map<String, String> dependencyDescriptor = new HashMap<>();
						dependencyDescriptor.put("path", projectDependency.getDependencyProject().getPath());
						dependencyDescriptor.put("configuration", "mavenRepository");
						mavenRepository.getDependencies().add(project.getDependencies().project(dependencyDescriptor));
					}
				});
		PublishingExtension publishing = project.getExtensions().getByType(PublishingExtension.class);
		publishing.getPublications().create("maven", MavenPublication.class, (publication) -> {
			SoftwareComponent javaComponent = project.getComponents().findByName("java");
			if (javaComponent != null) {
				publication.from(javaComponent);
			}
		});
		Copy populateLocalMavenRepository = project.getTasks().create("populateLocalMavenRepository", Copy.class);
		populateLocalMavenRepository.setDestinationDir(project.getBuildDir());
		populateLocalMavenRepository.into("local-maven-repository", (copy) -> copy.from(mavenRepository)
				.from(new File(project.getBuildDir(), "internal-maven-repository")));
		populateLocalMavenRepository
				.dependsOn(project.getTasks().getByName("publishMavenPublicationToInternalRepository"));
		project.getTasks().getByName("test").dependsOn(populateLocalMavenRepository);
		configurePomPackaging(project);
		MavenExec generateHelpMojo = configureMojoGenerationTasks(project);
		MavenExec generatePluginDescriptor = configurePluginDescriptorGenerationTasks(project, generateHelpMojo);
		DocumentPluginGoals documentPluginGoals = project.getTasks().create("documentPluginGoals",
				DocumentPluginGoals.class);
		documentPluginGoals.setPluginXml(generatePluginDescriptor.getOutputs().getFiles().getSingleFile());
		documentPluginGoals.setOutputDir(new File(project.getBuildDir(), "docs/generated/goals/"));
		documentPluginGoals.dependsOn(generatePluginDescriptor);
		Jar jar = (Jar) project.getTasks().getByName(JavaPlugin.JAR_TASK_NAME);
		includeDescriptorInJar(jar, generatePluginDescriptor);
		includeHelpMojoInJar(jar, generateHelpMojo);
		PrepareMavenBinaries prepareMavenBinaries = project.getTasks().create("prepareMavenBinaries",
				PrepareMavenBinaries.class);
		prepareMavenBinaries.setOutputDir(new File(project.getBuildDir(), "maven-binaries"));
	}

	private void configurePomPackaging(Project project) {
		PublishingExtension publishing = project.getExtensions().getByType(PublishingExtension.class);
		publishing.getPublications().withType(MavenPublication.class,
				(mavenPublication) -> mavenPublication.pom((pom) -> pom.setPackaging("maven-plugin")));
	}

	private MavenExec configureMojoGenerationTasks(Project project) {
		File helpMojoDir = new File(project.getBuildDir(), "help-mojo");
		Copy helpMojoInputs = createCopyHelpMojoInputs(project, helpMojoDir);
		MavenExec generateHelpMojo = createGenerateHelpMojo(project, helpMojoDir);
		generateHelpMojo.dependsOn(helpMojoInputs);
		return generateHelpMojo;
	}

	private Copy createCopyHelpMojoInputs(Project project, File mavenDir) {
		Copy mojoInputs = project.getTasks().create("copyHelpMojoInputs", Copy.class);
		mojoInputs.setDestinationDir(mavenDir);
		mojoInputs.from(new File(project.getProjectDir(), "src/maven/resources/pom.xml"),
				(sync) -> sync.filter((input) -> input.replace("{{version}}", project.getVersion().toString())));
		return mojoInputs;
	}

	private MavenExec createGenerateHelpMojo(Project project, File mavenDir) {
		MavenExec generateHelpMojo = project.getTasks().create("generateHelpMojo", MavenExec.class);
		generateHelpMojo.setProjectDir(mavenDir);
		generateHelpMojo.args("org.apache.maven.plugins:maven-plugin-plugin:3.6.0:helpmojo");
		generateHelpMojo.getOutputs().dir(new File(mavenDir, "target/generated-sources/plugin"));
		return generateHelpMojo;
	}

	private MavenExec configurePluginDescriptorGenerationTasks(Project project, MavenExec generateHelpMojo) {
		File pluginDescriptorDir = new File(project.getBuildDir(), "plugin-descriptor");
		SourceSetContainer sourceSets = project.getConvention().getPlugin(JavaPluginConvention.class).getSourceSets();
		SourceSet mainSourceSet = sourceSets.getByName(SourceSet.MAIN_SOURCE_SET_NAME);
		SourceSet helpMojoSourceSet = sourceSets.create("helpMojo");
		helpMojoSourceSet.getJava()
				.srcDir(new File(generateHelpMojo.getProjectDir(), "target/generated-sources/plugin"));
		project.getTasks().getByName(helpMojoSourceSet.getCompileJavaTaskName()).dependsOn(generateHelpMojo);
		Copy pluginDescriptorInputs = createCopyPluginDescriptorInputs(project, pluginDescriptorDir, mainSourceSet,
				helpMojoSourceSet);
		pluginDescriptorInputs.dependsOn(mainSourceSet.getClassesTaskName());
		pluginDescriptorInputs.dependsOn(helpMojoSourceSet.getClassesTaskName());
		MavenExec generatePluginDescriptor = createGeneratePluginDescriptor(project, pluginDescriptorDir);
		generatePluginDescriptor.dependsOn(pluginDescriptorInputs);
		return generatePluginDescriptor;
	}

	private Copy createCopyPluginDescriptorInputs(Project project, File destination, SourceSet... sourceSets) {
		Copy pluginDescriptorInputs = project.getTasks().create("copyPluginDescriptorInputs", Copy.class);
		pluginDescriptorInputs.setDestinationDir(destination);
		pluginDescriptorInputs.from(new File(project.getProjectDir(), "src/maven/resources/pom.xml"),
				(sync) -> sync.filter((input) -> input.replace("{{version}}", project.getVersion().toString())));
		for (SourceSet sourceSet : sourceSets) {
			pluginDescriptorInputs.from(sourceSet.getOutput().getClassesDirs(), (sync) -> sync.into("target/classes"));
			pluginDescriptorInputs.from(sourceSet.getAllJava().getSrcDirs(), (sync) -> sync.into("src/main/java"));
		}
		pluginDescriptorInputs.from(new File(project.getProjectDir(), "src/maven/resources/pom.xml"),
				(sync) -> sync.filter((input) -> input.replace("{{version}}", project.getVersion().toString())));
		return pluginDescriptorInputs;
	}

	private MavenExec createGeneratePluginDescriptor(Project project, File mavenDir) {
		MavenExec generatePluginDescriptor = project.getTasks().create("generatePluginDescriptor", MavenExec.class);
		generatePluginDescriptor.args("org.apache.maven.plugins:maven-plugin-plugin:3.6.0:descriptor");
		generatePluginDescriptor.getOutputs().file(new File(mavenDir, "target/classes/META-INF/maven/plugin.xml"));
		generatePluginDescriptor.getInputs().dir(new File(mavenDir, "target/classes/org"));
		generatePluginDescriptor.setProjectDir(mavenDir);
		return generatePluginDescriptor;
	}

	private void includeDescriptorInJar(Jar jar, JavaExec generatePluginDescriptor) {
		jar.from(generatePluginDescriptor, (copy) -> copy.into("META-INF/maven/"));
		jar.dependsOn(generatePluginDescriptor);
	}

	private void includeHelpMojoInJar(Jar jar, JavaExec generateHelpMojo) {
		jar.from(generateHelpMojo);
		jar.dependsOn(generateHelpMojo);
	}

}
