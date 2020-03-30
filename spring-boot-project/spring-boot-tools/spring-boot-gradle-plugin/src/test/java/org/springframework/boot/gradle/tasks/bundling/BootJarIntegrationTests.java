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
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.stream.Collectors;

import org.gradle.testkit.runner.BuildResult;
import org.gradle.testkit.runner.InvalidRunnerConfigurationException;
import org.gradle.testkit.runner.TaskOutcome;
import org.gradle.testkit.runner.UnexpectedBuildFailure;
import org.junit.jupiter.api.TestTemplate;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests for {@link BootJar}.
 *
 * @author Andy Wilkinson
 * @author Madhura Bhave
 */
class BootJarIntegrationTests extends AbstractBootArchiveIntegrationTests {

	BootJarIntegrationTests() {
		super("bootJar");
	}

	@TestTemplate
	void upToDateWhenBuiltTwiceWithLayers() throws InvalidRunnerConfigurationException, UnexpectedBuildFailure {
		assertThat(this.gradleBuild.build("-Playered=true", "bootJar").task(":bootJar").getOutcome())
				.isEqualTo(TaskOutcome.SUCCESS);
		assertThat(this.gradleBuild.build("-Playered=true", "bootJar").task(":bootJar").getOutcome())
				.isEqualTo(TaskOutcome.UP_TO_DATE);
	}

	@TestTemplate
	void notUpToDateWhenBuiltWithoutLayersAndThenWithLayers()
			throws InvalidRunnerConfigurationException, UnexpectedBuildFailure {
		assertThat(this.gradleBuild.build("bootJar").task(":bootJar").getOutcome()).isEqualTo(TaskOutcome.SUCCESS);
		assertThat(this.gradleBuild.build("-Playered=true", "bootJar").task(":bootJar").getOutcome())
				.isEqualTo(TaskOutcome.SUCCESS);
	}

	@TestTemplate
	void notUpToDateWhenBuiltWithLayersAndToolsAndThenWithLayersAndWithoutTools()
			throws InvalidRunnerConfigurationException, UnexpectedBuildFailure {
		assertThat(this.gradleBuild.build("-Playered=true", "bootJar").task(":bootJar").getOutcome())
				.isEqualTo(TaskOutcome.SUCCESS);
		assertThat(this.gradleBuild.build("-Playered=true", "-PexcludeTools=true", "bootJar").task(":bootJar")
				.getOutcome()).isEqualTo(TaskOutcome.SUCCESS);
	}

	@TestTemplate
	void implicitLayers() throws IOException {
		writeMainClass();
		writeResource();
		BuildResult result = this.gradleBuild.build("bootJar");
		assertThat(result.task(":bootJar").getOutcome()).isEqualTo(TaskOutcome.SUCCESS);
		try (JarFile jarFile = new JarFile(new File(this.gradleBuild.getProjectDir(), "build/libs").listFiles()[0])) {
			Set<String> entryNames = Collections.list(jarFile.entries()).stream()
					.filter((entry) -> !entry.isDirectory()).map(JarEntry::getName)
					.filter((name) -> !"BOOT-INF/layers.idx".equals(name)).collect(Collectors.toSet());
			List<String> index = entryLines(jarFile, "BOOT-INF/layers.idx");
			Set<String> indexed = index.stream().map((entry) -> entry.substring(entry.indexOf(' ') + 1))
					.collect(Collectors.toSet());
			Set<String> unindexed = new HashSet<>(entryNames);
			unindexed.removeAll(indexed);
			assertThat(unindexed).isEmpty();
			assertThat(index).containsSubsequence("dependencies BOOT-INF/lib/commons-lang3-3.9.jar",
					"dependencies BOOT-INF/lib/spring-boot-jarmode-layertools.jar",
					"snapshot-dependencies BOOT-INF/lib/commons-io-2.7-SNAPSHOT.jar",
					"application org/springframework/boot/loader/JarLauncher.class",
					"application BOOT-INF/classes/example/Main.class", "application BOOT-INF/classes/static/file.txt");
		}
	}

	@TestTemplate
	void customLayers() throws IOException {
		writeMainClass();
		writeResource();
		BuildResult result = this.gradleBuild.build("bootJar");
		System.out.println(result.getOutput());
		assertThat(result.task(":bootJar").getOutcome()).isEqualTo(TaskOutcome.SUCCESS);
		try (JarFile jarFile = new JarFile(new File(this.gradleBuild.getProjectDir(), "build/libs").listFiles()[0])) {
			Set<String> entryNames = Collections.list(jarFile.entries()).stream()
					.filter((entry) -> !entry.isDirectory()).map(JarEntry::getName)
					.filter((name) -> !"BOOT-INF/layers.idx".equals(name)).collect(Collectors.toSet());
			List<String> index = entryLines(jarFile, "BOOT-INF/layers.idx");
			Set<String> indexed = index.stream().map((entry) -> entry.substring(entry.indexOf(' ') + 1))
					.collect(Collectors.toSet());
			Set<String> unindexed = new HashSet<>(entryNames);
			unindexed.removeAll(indexed);
			assertThat(unindexed).isEmpty();
			assertThat(index).containsSubsequence("dependencies BOOT-INF/lib/spring-boot-jarmode-layertools.jar",
					"commons-dependencies BOOT-INF/lib/commons-lang3-3.9.jar",
					"snapshot-dependencies BOOT-INF/lib/commons-io-2.7-SNAPSHOT.jar",
					"static BOOT-INF/classes/static/file.txt", "app META-INF/MANIFEST.MF",
					"app org/springframework/boot/loader/JarLauncher.class", "app BOOT-INF/classes/example/Main.class");
		}
	}

	private void writeMainClass() {
		File examplePackage = new File(this.gradleBuild.getProjectDir(), "src/main/java/example");
		examplePackage.mkdirs();
		File main = new File(examplePackage, "Main.java");
		try (PrintWriter writer = new PrintWriter(new FileWriter(main))) {
			writer.println("package example;");
			writer.println();
			writer.println("import java.io.IOException;");
			writer.println();
			writer.println("public class Main {");
			writer.println();
			writer.println("    public static void main(String[] args) {");
			writer.println("    }");
			writer.println();
			writer.println("}");
		}
		catch (IOException ex) {
			throw new RuntimeException(ex);
		}
	}

	private void writeResource() {
		try {
			Path path = this.gradleBuild.getProjectDir().toPath()
					.resolve(Paths.get("src", "main", "resources", "static", "file.txt"));
			Files.createDirectories(path.getParent());
			Files.createFile(path);
		}
		catch (IOException ex) {
			throw new RuntimeException(ex);
		}
	}

	private List<String> entryLines(JarFile jarFile, String entryName) throws IOException {
		try (BufferedReader reader = new BufferedReader(
				new InputStreamReader(jarFile.getInputStream(jarFile.getEntry(entryName))))) {
			return reader.lines().collect(Collectors.toList());
		}
	}

}
