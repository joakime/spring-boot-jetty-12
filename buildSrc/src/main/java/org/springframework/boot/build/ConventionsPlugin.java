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

package org.springframework.boot.build;

import io.spring.javaformat.gradle.SpringJavaFormatPlugin;
import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.gradle.api.artifacts.DependencySet;
import org.gradle.api.component.SoftwareComponent;
import org.gradle.api.plugins.JavaPlugin;
import org.gradle.api.plugins.JavaPluginExtension;
import org.gradle.api.plugins.quality.CheckstyleExtension;
import org.gradle.api.plugins.quality.CheckstylePlugin;
import org.gradle.api.publish.PublicationContainer;
import org.gradle.api.publish.PublishingExtension;
import org.gradle.api.publish.maven.MavenPom;
import org.gradle.api.publish.maven.MavenPomDeveloperSpec;
import org.gradle.api.publish.maven.MavenPomIssueManagement;
import org.gradle.api.publish.maven.MavenPomLicenseSpec;
import org.gradle.api.publish.maven.MavenPomOrganization;
import org.gradle.api.publish.maven.MavenPomScm;
import org.gradle.api.publish.maven.MavenPublication;
import org.gradle.api.publish.maven.plugins.MavenPublishPlugin;
import org.gradle.api.tasks.testing.Test;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/**
 * Plugin to apply conventions to projects that are part of Spring Boot's build.
 *
 * @author Andy Wilkinson
 */
public class ConventionsPlugin implements Plugin<Project> {

	@Override
	public void apply(Project project) {
		applyJavaConventions(project);
		applyTestConventions(project);
		applyAsciidoctorConventions(project);
		applyMavenPublishingConventions(project);
	}

	private void applyJavaConventions(Project project) {
		project.getPlugins().withType(JavaPlugin.class, (java) -> {
			configureSpringJavaFormat(project);
			project.setProperty("sourceCompatibility", "1.8");
		});
	}

	private void configureSpringJavaFormat(Project project) {
		project.getPlugins().apply(SpringJavaFormatPlugin.class);
		project.getPlugins().apply(CheckstylePlugin.class);
		CheckstyleExtension checkstyle = project.getExtensions().getByType(CheckstyleExtension.class);
		checkstyle.setToolVersion("8.22");
		checkstyle.setConfigDir(project.getRootProject().file("src/checkstyle"));
		String version = SpringJavaFormatPlugin.class.getPackage().getImplementationVersion();
		DependencySet checkstyleDependencies = project.getConfigurations().getByName("checkstyle").getDependencies();
		checkstyleDependencies
				.add(project.getDependencies().create("io.spring.javaformat:spring-javaformat-checkstyle:" + version));
		checkstyleDependencies
				.add(project.getDependencies().create("io.spring.nohttp:nohttp-checkstyle:0.0.3.RELEASE"));
	}

	private void applyTestConventions(Project project) {
		project.getTasks().withType(Test.class, (test) -> {
			test.useJUnitPlatform();
			test.setMaxHeapSize("1024M");
		});
	}

	private void applyAsciidoctorConventions(Project project) {
		new AsciidoctorConventions().apply(project);
	}

	private void applyMavenPublishingConventions(Project project) {
		project.getPlugins().withType(MavenPublishPlugin.class, (plugin) -> {
			project.getPlugins().withType(JavaPlugin.class, (java) -> {
				JavaPluginExtension javaExtension = project.getExtensions().getByType(JavaPluginExtension.class);
				javaExtension.withJavadocJar();
				javaExtension.withSourcesJar();
			});
			customizeMavenPublishing(project);
		});
	}

	private void customizeMavenPublishing(Project project) {
		PublishingExtension publishing = project.getExtensions().getByType(PublishingExtension.class);
		if (project.hasProperty("deploymentRepository")) {
			publishing.getRepositories()
					.maven((mavenRepository) -> mavenRepository.setUrl(project.property("deploymentRepository")));
		}
		PublicationContainer publications = publishing.getPublications();
		project.afterEvaluate((evaluatedProject) -> {
			if (publications.isEmpty()) {
				publishing.getPublications().create("maven", MavenPublication.class, (publication) -> {
					SoftwareComponent javaComponent = project.getComponents().findByName("java");
					if (javaComponent != null) {
						publication.from(javaComponent);
						publication.versionMapping((strategy) -> strategy.usage("java-runtime",
								(variantStrategy) -> variantStrategy.fromResolutionResult()));
					}
				});
			}
		});
		publications.withType(MavenPublication.class)
				.all((publication) -> publication.pom((pom) -> customizePom(pom, project)));
	}

	private void customizePom(MavenPom pom, Project project) {
		pom.getUrl().set("https://projects.spring.io/spring-boot/#");
		pom.getDescription().set(project.provider(project::getDescription));
		pom.organization(this::customizeOrganization);
		pom.licenses(this::customizeLicences);
		pom.developers(this::customizeDevelopers);
		pom.scm(this::customizeScm);
		pom.issueManagement(this::customizeIssueManagement);
		pom.withXml((xmlProvider) -> {
			Element pomElement = xmlProvider.asElement();
			NodeList children = pomElement.getChildNodes();
			for (int i = 0; i < children.getLength(); i++) {
				Node child = children.item(i);
				if ("dependencyManagement".equals(child.getNodeName())) {
					pomElement.removeChild(child);
				}
			}
		});
	}

	private void customizeOrganization(MavenPomOrganization organization) {
		organization.getName().set("Pivotal Software, Inc.");
		organization.getUrl().set("https://spring.io");
	}

	private void customizeLicences(MavenPomLicenseSpec licences) {
		licences.license((licence) -> {
			licence.getName().set("Apache License, Version 2.0");
			licence.getUrl().set("http://www.apache.org/licenses/LICENSE-2.0");
		});
	}

	private void customizeDevelopers(MavenPomDeveloperSpec developers) {
		developers.developer((developer) -> {
			developer.getName().set("Pivotal");
			developer.getEmail().set("info@pivotal.io");
			developer.getOrganization().set("Pivotal Software, Inc.");
			developer.getOrganizationUrl().set("https://www.spring.io");
		});
	}

	private void customizeScm(MavenPomScm scm) {
		scm.getConnection().set("scm:git:git://github.com/spring-projects/spring-boot.git");
		scm.getDeveloperConnection().set("scm:git:ssh://git@github.com/spring-projects/spring-boot.git");
		scm.getUrl().set("https://github.com/spring-projects/spring-boot");

	}

	private void customizeIssueManagement(MavenPomIssueManagement issueManagement) {
		issueManagement.getSystem().set("GitHub");
		issueManagement.getUrl().set("https://github.com/spring-projects/spring-boot/issues");
	}

}
