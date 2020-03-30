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

package org.springframework.boot.gradle.tasks.bundling;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.List;
import java.util.jar.JarFile;
import java.util.stream.Collectors;

import org.gradle.api.Action;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link BootJar}.
 *
 * @author Andy Wilkinson
 * @author Madhura Bhave
 * @author Scott Frederick
 */
class BootJarTests extends AbstractBootArchiveTests<BootJar> {

	BootJarTests() {
		super(BootJar.class, "org.springframework.boot.loader.JarLauncher", "BOOT-INF/lib/", "BOOT-INF/classes/");
	}

	@Test
	void contentCanBeAddedToBootInfUsingCopySpecFromGetter() throws IOException {
		BootJar bootJar = getTask();
		bootJar.setMainClassName("com.example.Application");
		bootJar.getBootInf().into("test").from(new File("build.gradle").getAbsolutePath());
		bootJar.copy();
		try (JarFile jarFile = new JarFile(bootJar.getArchiveFile().get().getAsFile())) {
			assertThat(jarFile.getJarEntry("BOOT-INF/test/build.gradle")).isNotNull();
		}
	}

	@Test
	void contentCanBeAddedToBootInfUsingCopySpecAction() throws IOException {
		BootJar bootJar = getTask();
		bootJar.setMainClassName("com.example.Application");
		bootJar.bootInf((copySpec) -> copySpec.into("test").from(new File("build.gradle").getAbsolutePath()));
		bootJar.copy();
		try (JarFile jarFile = new JarFile(bootJar.getArchiveFile().get().getAsFile())) {
			assertThat(jarFile.getJarEntry("BOOT-INF/test/build.gradle")).isNotNull();
		}
	}

	@Test
	void whenJarIsLayeredThenManifestContainsEntryForLayersIndex() throws IOException {
		try (JarFile jarFile = new JarFile(createLayeredJar())) {
			assertThat(jarFile.getManifest().getMainAttributes().getValue("Spring-Boot-Layers-Index"))
					.isEqualTo("BOOT-INF/layers.idx");
		}
	}

	@Test
	void whenJarIsLayeredThenLayersIndexIsPresentAndListsLayersInOrder() throws IOException {
		try (JarFile jarFile = new JarFile(createLayeredJar())) {
			assertThat(entryLines(jarFile, "BOOT-INF/layers.idx")).containsSubsequence(
					"dependencies BOOT-INF/lib/first-library.jar",
					"snapshot-dependencies BOOT-INF/lib/third-library-SNAPSHOT.jar",
					"application org/springframework/boot/loader/JarLauncher.class");
		}
	}

	@Test
	void whenJarIsLayeredThenLayerToolsAreAddedToTheJar() throws IOException {
		List<String> entryNames = getEntryNames(createLayeredJar());
		assertThat(entryNames).contains("BOOT-INF/lib/spring-boot-jarmode-layertools.jar");
	}

	@Test
	void whenJarIsLayeredAndIncludeLayerToolsIsFalseThenLayerToolsAreNotAddedToTheJar() throws IOException {
		List<String> entryNames = getEntryNames(
				createLayeredJar((configuration) -> configuration.setIncludeLayerTools(false)));
		assertThat(entryNames).doesNotContain("BOOT-INF/lib/spring-boot-jarmode-layertools.jar");
	}

	@Test
	void classpathIndexPointsToBootInfLibs() throws IOException {
		try (JarFile jarFile = new JarFile(createPopulatedJar())) {
			assertThat(jarFile.getManifest().getMainAttributes().getValue("Spring-Boot-Classpath-Index"))
					.isEqualTo("BOOT-INF/classpath.idx");
			assertThat(entryLines(jarFile, "BOOT-INF/classpath.idx")).containsExactly("first-library.jar",
					"second-library.jar", "third-library-SNAPSHOT.jar");
		}
	}

	private File createPopulatedJar() throws IOException {
		addContent();
		executeTask();
		return getTask().getArchiveFile().get().getAsFile();
	}

	private File createLayeredJar(Action<LayerConfiguration> action) throws IOException {
		if (action != null) {
			getTask().layers(action);
		}
		else {
			getTask().layers();
		}
		addContent();
		executeTask();
		return getTask().getArchiveFile().get().getAsFile();
	}

	private File createLayeredJar() throws IOException {
		return createLayeredJar(null);
	}

	private void addContent() throws IOException {
		BootJar bootJar = getTask();
		bootJar.setMainClassName("com.example.Main");
		File classesJavaMain = new File(this.temp, "classes/java/main");
		File applicationClass = new File(classesJavaMain, "com/example/Application.class");
		applicationClass.getParentFile().mkdirs();
		applicationClass.createNewFile();
		File resourcesMain = new File(this.temp, "resources/main");
		File applicationProperties = new File(resourcesMain, "application.properties");
		applicationProperties.getParentFile().mkdirs();
		applicationProperties.createNewFile();
		File staticResources = new File(resourcesMain, "static");
		staticResources.mkdir();
		File css = new File(staticResources, "test.css");
		css.createNewFile();
		bootJar.classpath(classesJavaMain, resourcesMain, jarFile("first-library.jar"), jarFile("second-library.jar"),
				jarFile("third-library-SNAPSHOT.jar"));
	}

	private List<String> entryLines(JarFile jarFile, String entryName) throws IOException {
		try (BufferedReader reader = new BufferedReader(
				new InputStreamReader(jarFile.getInputStream(jarFile.getEntry(entryName))))) {
			return reader.lines().collect(Collectors.toList());
		}
	}

	@Override
	protected void executeTask() {
		getTask().copy();
	}

}
