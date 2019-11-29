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
package org.springframework.boot.maven;

import java.io.File;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests for the Maven plugin's jar support.
 *
 * @author Andy Wilkinson
 */
class JarIntegrationTests extends AbstractArchiveIntegrationTests {

	@Test
	void whenJarIsRepackagedInPlaceOnlyRepackagedJarIsInstalled() {
		new MavenBuild("jar").goals("install").execute((project) -> {
			File original = new File(project, "target/jar-0.0.1.BUILD-SNAPSHOT.jar.original");
			assertThat(original).isFile();
			File repackaged = new File(project, "target/jar-0.0.1.BUILD-SNAPSHOT.jar");
			assertThat(launchScript(repackaged)).isEmpty();
			assertThat(jar(repackaged)).manifest((manifest) -> {
				manifest.hasMainClass("org.springframework.boot.loader.JarLauncher");
				manifest.hasStartClass("some.random.Main");
				manifest.hasAttribute("Not-Used", "Foo");
			}).hasEntryWithNameStartingWith("BOOT-INF/lib/spring-context")
					.hasEntryWithNameStartingWith("BOOT-INF/lib/spring-core")
					.hasEntryWithNameStartingWith("BOOT-INF/lib/spring-jcl")
					.hasEntryWithNameStartingWith("BOOT-INF/lib/jakarta.servlet-api-4")
					.hasEntryWithName("BOOT-INF/classes/org/test/SampleApplication.class")
					.hasEntryWithName("org/springframework/boot/loader/JarLauncher.class");
			assertThat(buildLog(project)).contains("Replacing main artifact with repackaged archive")
					.contains("Installing " + repackaged + " to").doesNotContain("Installing " + original + " to");
		});
	}

	@Test
	void whenAttachIsDisabledOnlyTheOriginalJarIsInstalled() {
		new MavenBuild("jar-attach-disabled").goals("install").execute((project) -> {
			File original = new File(project, "target/jar-attach-disabled-0.0.1.BUILD-SNAPSHOT.jar.original");
			assertThat(original).isFile();
			File main = new File(project, "target/jar-attach-disabled-0.0.1.BUILD-SNAPSHOT.jar");
			assertThat(main).isFile();
			assertThat(buildLog(project)).contains("Updating main artifact " + main + " to " + original)
					.contains("Installing " + original + " to").doesNotContain("Installing " + main + " to");
		});
	}

	@Test
	void whenAClassifierIsConfiguredTheRepackagedJarHasAClassifierAndBothItAndTheOriginalAreInstalled() {
		new MavenBuild("jar-classifier-main").goals("install").execute((project) -> {
			assertThat(new File(project, "target/jar-classifier-main-0.0.1.BUILD-SNAPSHOT.jar.original"))
					.doesNotExist();
			File main = new File(project, "target/jar-classifier-main-0.0.1.BUILD-SNAPSHOT.jar");
			assertThat(main).isFile();
			File repackaged = new File(project, "target/jar-classifier-main-0.0.1.BUILD-SNAPSHOT-test.jar");
			assertThat(jar(repackaged)).hasEntryWithNameStartingWith("BOOT-INF/classes/");
			assertThat(buildLog(project))
					.contains("Attaching repackaged archive " + repackaged + " with classifier test")
					.doesNotContain("Creating repackaged archive " + repackaged + " with classifier test")
					.contains("Installing " + main + " to").contains("Installing " + repackaged + " to");
		});
	}

	@Test
	void whenBothJarsHaveTheSameClassifierRepackagingIsDoneInPlaceAndOnlyRepackagedJarIsInstalled() {
		new MavenBuild("jar-classifier-source").goals("install").execute((project) -> {
			File original = new File(project, "target/jar-classifier-source-0.0.1.BUILD-SNAPSHOT-test.jar.original");
			assertThat(original).isFile();
			File repackaged = new File(project, "target/jar-classifier-source-0.0.1.BUILD-SNAPSHOT-test.jar");
			assertThat(jar(repackaged)).hasEntryWithNameStartingWith("BOOT-INF/classes/");
			assertThat(buildLog(project)).contains("Replacing artifact with classifier test with repackaged archive")
					.doesNotContain("Installing " + original + " to").contains("Installing " + repackaged + " to");
		});
	}

	@Test
	void whenBothJarsHaveTheSameClassifierAndAttachIsDisabledOnlyTheOriginalJarIsInstalled() {
		new MavenBuild("jar-classifier-source-attach-disabled").goals("install").execute((project) -> {
			File original = new File(project,
					"target/jar-classifier-source-attach-disabled-0.0.1.BUILD-SNAPSHOT-test.jar.original");
			assertThat(original).isFile();
			File repackaged = new File(project,
					"target/jar-classifier-source-attach-disabled-0.0.1.BUILD-SNAPSHOT-test.jar");
			assertThat(jar(repackaged)).hasEntryWithNameStartingWith("BOOT-INF/classes/");
			assertThat(buildLog(project))
					.doesNotContain("Attaching repackaged archive " + repackaged + " with classifier test")
					.contains("Updating artifact with classifier test " + repackaged + " to " + original)
					.contains("Installing " + original + " to").doesNotContain("Installing " + repackaged + " to");
		});
	}

	@Test
	void whenAClassifierAndAnOutputDirectoryAreConfiguredTheRepackagedJarHasAClassifierAndIsWrittenToTheOutputDirectory() {
		new MavenBuild("jar-create-dir").goals("install").execute((project) -> {
			File repackaged = new File(project, "target/foo/jar-create-dir-0.0.1.BUILD-SNAPSHOT-foo.jar");
			assertThat(jar(repackaged)).hasEntryWithNameStartingWith("BOOT-INF/classes/");
			assertThat(buildLog(project)).contains("Installing " + repackaged + " to");
		});
	}

	@Test
	void whenAnOutputDirectoryIsConfiguredTheRepackagedJarIsWrittenToIt() {
		new MavenBuild("jar-custom-dir").goals("install").execute((project) -> {
			File repackaged = new File(project, "target/foo/jar-custom-dir-0.0.1.BUILD-SNAPSHOT.jar");
			assertThat(jar(repackaged)).hasEntryWithNameStartingWith("BOOT-INF/classes/");
			assertThat(buildLog(project)).contains("Installing " + repackaged + " to");
		});
	}

	@Test
	void whenACustomLaunchScriptIsConfiguredItAppearsInTheRepackagedJar() {
		new MavenBuild("jar-custom-launcher").goals("install").execute((project) -> {
			File repackaged = new File(project, "target/jar-0.0.1.BUILD-SNAPSHOT.jar");
			assertThat(jar(repackaged)).hasEntryWithNameStartingWith("BOOT-INF/classes/");
			assertThat(launchScript(repackaged)).contains("Hello world");
		});
	}

	@Test
	void whenAnEntryIsExcludedItDoesNotAppearInTheRepackagedJar() {
		new MavenBuild("jar-exclude-entry").goals("install").execute((project) -> {
			File repackaged = new File(project, "target/jar-exclude-entry-0.0.1.BUILD-SNAPSHOT.jar");
			assertThat(jar(repackaged)).hasEntryWithNameStartingWith("BOOT-INF/classes/")
					.hasEntryWithNameStartingWith("BOOT-INF/lib/spring-context")
					.hasEntryWithNameStartingWith("BOOT-INF/lib/spring-core")
					.hasEntryWithNameStartingWith("BOOT-INF/lib/spring-jcl")
					.doesNotHaveEntryWithName("BOOT-INF/lib/servlet-api-2.5.jar");
		});
	}

	@Test
	void whenAGroupIsExcludedNoEntriesInThatGroupAppearInTheRepackagedJar() {
		new MavenBuild("jar-exclude-group").goals("install").execute((project) -> {
			File repackaged = new File(project, "target/jar-exclude-group-0.0.1.BUILD-SNAPSHOT.jar");
			assertThat(jar(repackaged)).hasEntryWithNameStartingWith("BOOT-INF/classes/")
					.hasEntryWithNameStartingWith("BOOT-INF/lib/spring-context")
					.hasEntryWithNameStartingWith("BOOT-INF/lib/spring-core")
					.hasEntryWithNameStartingWith("BOOT-INF/lib/spring-jcl")
					.doesNotHaveEntryWithName("BOOT-INF/lib/log4j-api-2.4.1.jar");
		});
	}

	@Test
	void whenAJarIsExecutableItBeginsWithTheDefaultLaunchScript() {
		new MavenBuild("jar-executable").execute((project) -> {
			File repackaged = new File(project, "target/jar-executable-0.0.1.BUILD-SNAPSHOT.jar");
			assertThat(jar(repackaged)).hasEntryWithNameStartingWith("BOOT-INF/classes/");
			assertThat(launchScript(repackaged)).contains("Spring Boot Startup Script")
					.contains("MyFullyExecutableJarName").contains("MyFullyExecutableJarDesc");
		});
	}

	@Test
	void whenAJarIsBuiltWithLibrariesWithConflictingNamesTheyAreMadeUniqueUsingTheirGroupIds() {
		new MavenBuild("jar-lib-name-conflict").execute((project) -> {
			File repackaged = new File(project, "test-project/target/test-project-0.0.1.BUILD-SNAPSHOT.jar");
			assertThat(jar(repackaged)).hasEntryWithNameStartingWith("BOOT-INF/classes/")
					.hasEntryWithName(
							"BOOT-INF/lib/org.springframework.boot.maven.it-acme-lib-0.0.1.BUILD-SNAPSHOT.jar")
					.hasEntryWithName(
							"BOOT-INF/lib/org.springframework.boot.maven.it.another-acme-lib-0.0.1.BUILD-SNAPSHOT.jar");
		});
	}

	@Test
	void whenAProjectUsesPomPackagingRepackagingIsSkipped() {
		new MavenBuild("jar-pom").execute((project) -> {
			File target = new File(project, "target");
			assertThat(target.listFiles()).containsExactly(new File(target, "build.log"));
		});
	}

	@Test
	void whenRepackagingIsSkippedTheJarIsNotRepackaged() {
		new MavenBuild("jar-skip").execute((project) -> {
			File main = new File(project, "target/jar-skip-0.0.1.BUILD-SNAPSHOT.jar");
			assertThat(jar(main)).doesNotHaveEntryWithNameStartingWith("org/springframework/boot");
			assertThat(new File(project, "target/jar-skip-0.0.1.BUILD-SNAPSHOT.jar.original")).doesNotExist();

		});
	}

	@Test
	void whenADependencyHasSystemScopeAndInclusionOfSystemScopeDependenciesIsEnabledItIsIncludedInTheRepackagedJar() {
		new MavenBuild("jar-system-scope").execute((project) -> {
			File main = new File(project, "target/jar-system-scope-0.0.1.BUILD-SNAPSHOT.jar");
			assertThat(jar(main)).hasEntryWithName("BOOT-INF/lib/sample-1.0.0.jar");

		});
	}

	@Test
	void whenADependencyHasSystemScopeItIsNotIncludedInTheRepackagedJar() {
		new MavenBuild("jar-system-scope-default").execute((project) -> {
			File main = new File(project, "target/jar-system-scope-default-0.0.1.BUILD-SNAPSHOT.jar");
			assertThat(jar(main)).doesNotHaveEntryWithName("BOOT-INF/lib/sample-1.0.0.jar");

		});
	}

	@Test
	void whenADependendencyHasTestScopeItIsNotIncludedInTheRepackagedJar() {
		new MavenBuild("jar-test-scope").execute((project) -> {
			File main = new File(project, "target/jar-test-scope-0.0.1.BUILD-SNAPSHOT.jar");
			assertThat(jar(main)).doesNotHaveEntryWithNameStartingWith("BOOT-INF/lib/log4j")
					.hasEntryWithNameStartingWith("BOOT-INF/lib/spring-");
		});
	}

	@Test
	void whenAProjectUsesKotlinItsModuleMetadataIsRepackagedIntoBootInfClasses() {
		new MavenBuild("jar-with-kotlin-module").execute((project) -> {
			File main = new File(project, "target/jar-with-kotlin-module-0.0.1.BUILD-SNAPSHOT.jar");
			assertThat(jar(main)).hasEntryWithName("BOOT-INF/classes/META-INF/jar-with-kotlin-module.kotlin_module");
		});
	}

	@Test
	void whenAProjectIsBuiltWithALayoutPropertyTheSpecifiedLayoutIsUsed() {
		new MavenBuild("jar-with-layout-property").goals("package", "-Dspring-boot.repackage.layout=ZIP")
				.execute((project) -> {
					File main = new File(project, "target/jar-with-layout-property-0.0.1.BUILD-SNAPSHOT.jar");
					assertThat(jar(main)).manifest(
							(manifest) -> manifest.hasMainClass("org.springframework.boot.loader.PropertiesLauncher")
									.hasStartClass("org.test.SampleApplication"));
					assertThat(buildLog(project)).contains("Layout: ZIP");
				});
	}

	@Test
	void whenALayoutIsConfiguredTheSpecifiedLayoutIsUsed() {
		new MavenBuild("jar-with-zip-layout").execute((project) -> {
			File main = new File(project, "target/jar-with-zip-layout-0.0.1.BUILD-SNAPSHOT.jar");
			assertThat(jar(main))
					.manifest((manifest) -> manifest.hasMainClass("org.springframework.boot.loader.PropertiesLauncher")
							.hasStartClass("org.test.SampleApplication"));
			assertThat(buildLog(project)).contains("Layout: ZIP");
		});
	}

	@Test
	void whenRequiresUnpackConfigurationIsProvidedItIsReflectedInTheRepackagedJar() {
		new MavenBuild("jar-with-unpack").execute((project) -> {
			File main = new File(project, "target/jar-with-unpack-0.0.1.BUILD-SNAPSHOT.jar");
			assertThat(jar(main)).hasUnpackEntryWithNameStartingWith("BOOT-INF/lib/spring-core-")
					.hasEntryWithNameStartingWith("BOOT-INF/lib/spring-context-");
		});
	}

}
