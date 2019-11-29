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
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import io.spring.javaformat.formatter.FileFormatter;
import org.gradle.api.DefaultTask;
import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.gradle.api.Task;
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
import org.gradle.api.tasks.OutputDirectory;
import org.gradle.api.tasks.SourceSet;
import org.gradle.api.tasks.SourceSetContainer;
import org.gradle.api.tasks.TaskAction;
import org.gradle.api.tasks.TaskExecutionException;
import org.gradle.api.tasks.bundling.Jar;
import org.gradle.api.tasks.javadoc.Javadoc;
import org.gradle.external.javadoc.StandardJavadocDocletOptions;

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
		File generatedHelpMojoDir = new File(project.getBuildDir(), "generated/sources/helpMojo");
		project.getTasks().withType(Javadoc.class,
				(javadoc) -> ((StandardJavadocDocletOptions) javadoc.getOptions()).addMultilineStringsOption("tag")
						.setValue(Arrays.asList("goal:X", "requiresProject:X", "threadSafe:X")));
		FormatHelpMojoSource copyFormattedHelpMojoSource = project.getTasks().create("copyFormattedHelpMojoSource",
				FormatHelpMojoSource.class);
		copyFormattedHelpMojoSource.setGenerator(generateHelpMojo);
		copyFormattedHelpMojoSource.setOutputDir(generatedHelpMojoDir);
		mainSourceSet.getAllJava().srcDir(generatedHelpMojoDir);
		project.getTasks().getByName(mainSourceSet.getCompileJavaTaskName()).dependsOn(copyFormattedHelpMojoSource);
		Copy pluginDescriptorInputs = createCopyPluginDescriptorInputs(project, pluginDescriptorDir, mainSourceSet);
		pluginDescriptorInputs.dependsOn(mainSourceSet.getClassesTaskName());
		MavenExec generatePluginDescriptor = createGeneratePluginDescriptor(project, pluginDescriptorDir);
		generatePluginDescriptor.dependsOn(pluginDescriptorInputs);
		return generatePluginDescriptor;
	}

	private Copy createCopyPluginDescriptorInputs(Project project, File destination, SourceSet sourceSet) {
		Copy pluginDescriptorInputs = project.getTasks().create("copyPluginDescriptorInputs", Copy.class);
		pluginDescriptorInputs.setDestinationDir(destination);
		pluginDescriptorInputs.from(new File(project.getProjectDir(), "src/maven/resources/pom.xml"),
				(sync) -> sync.filter((input) -> input.replace("{{version}}", project.getVersion().toString())));
		pluginDescriptorInputs.from(sourceSet.getOutput().getClassesDirs(), (sync) -> sync.into("target/classes"));
		pluginDescriptorInputs.from(sourceSet.getAllJava().getSrcDirs(), (sync) -> sync.into("src/main/java"));
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

	public static class FormatHelpMojoSource extends DefaultTask {

		private Task generator;

		private File outputDir;

		void setGenerator(Task generator) {
			this.generator = generator;
			getInputs().files(this.generator);
		}

		@OutputDirectory
		public File getOutputDir() {
			return this.outputDir;
		}

		void setOutputDir(File outputDir) {
			this.outputDir = outputDir;
		}

		@TaskAction
		void syncAndFormat() {
			FileFormatter fileFormatter = new FileFormatter();
			for (File output : this.generator.getOutputs().getFiles()) {
				fileFormatter.formatFiles(getProject().fileTree(output), StandardCharsets.UTF_8).forEach((fileEdit) -> {
					Path relativePath = output.toPath().relativize(fileEdit.getFile().toPath());
					Path outputLocation = this.outputDir.toPath().resolve(relativePath);
					try {
						Files.createDirectories(outputLocation.getParent());
						Files.write(outputLocation, fileEdit.getFormattedContent().getBytes(StandardCharsets.UTF_8));
					}
					catch (Exception ex) {
						throw new TaskExecutionException(this, ex);
					}
				});
			}
		}

	}

}
