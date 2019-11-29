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
 * Integration tests for the Maven plugin's war support.
 *
 * @author Andy Wilkinson
 */
class WarIntegrationTests extends AbstractArchiveIntegrationTests {

	@Test
	void warRepackaging() {
		new MavenBuild("war")
				.execute((project) -> assertThat(jar(new File(project, "target/war-0.0.1.BUILD-SNAPSHOT.war")))
						.hasEntryWithNameStartingWith("WEB-INF/lib/spring-context")
						.hasEntryWithNameStartingWith("WEB-INF/lib/spring-core")
						.hasEntryWithNameStartingWith("WEB-INF/lib/spring-jcl")
						.hasEntryWithNameStartingWith("WEB-INF/lib-provided/jakarta.servlet-api-4")
						.hasEntryWithName("org/springframework/boot/loader/WarLauncher.class")
						.hasEntryWithName("WEB-INF/classes/org/test/SampleApplication.class")
						.hasEntryWithName("index.html")
						.manifest((manifest) -> manifest.hasMainClass("org.springframework.boot.loader.WarLauncher")
								.hasStartClass("org.test.SampleApplication").hasAttribute("Not-Used", "Foo")));
	}

	@Test
	void jarDependencyWithCustomFinalNameBuiltInSameReactorIsPackagedUsingArtifactIdAndVersion() {
		new MavenBuild("war-reactor")
				.execute(((project) -> assertThat(jar(new File(project, "war/target/war-0.0.1.BUILD-SNAPSHOT.war")))
						.hasEntryWithName("WEB-INF/lib/jar-0.0.1.BUILD-SNAPSHOT.jar")
						.doesNotHaveEntryWithName("WEB-INF/lib/jar.jar")));
	}

	@Test
	void whenRequiresUnpackConfigurationIsProvidedItIsReflectedInTheRepackagedWar() {
		new MavenBuild("war-with-unpack").execute(
				(project) -> assertThat(jar(new File(project, "target/war-with-unpack-0.0.1.BUILD-SNAPSHOT.war")))
						.hasUnpackEntryWithNameStartingWith("WEB-INF/lib/spring-core-")
						.hasEntryWithNameStartingWith("WEB-INF/lib/spring-context-")
						.hasEntryWithNameStartingWith("WEB-INF/lib/spring-jcl-"));
	}

}
