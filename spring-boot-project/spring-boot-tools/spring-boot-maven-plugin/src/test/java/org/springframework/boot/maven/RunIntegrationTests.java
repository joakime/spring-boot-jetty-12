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

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.contentOf;

import java.io.File;
import java.lang.reflect.Method;

import org.junit.jupiter.api.Test;

/**
 * Integration tests for the Maven plugin's run goal.
 * 
 * @author Andy Wilkinson
 */
class RunIntegrationTests {
	
	@Test
	void whenTheRunGoalIsExecutedTheApplicationIsForkedWithOptimizedJvmArguments() {
		new MavenBuild("run").goals("spring-boot:run", "-X").execute((project) -> {
			String jvmArguments = isJava13OrLater() ? "JVM argument(s): -XX:TieredStopAtLevel=1" : "JVM argument(s): -Xverify:none -XX:TieredStopAtLevel=1";
			assertThat(buildLog(project)).contains("I haz been run").contains(jvmArguments);
		});
	}
	
	@Test
	void whenForkingIsDisabledAndDevToolsIsPresentDevToolsIsDisabled() {
		new MavenBuild("run-devtools").goals("spring-boot:run").execute((project) -> {
			assertThat(buildLog(project)).contains("I haz been run").contains("Fork mode disabled, devtools will be disabled");
		});
	}
	
	@Test
	void whenForkingIsDisabledJvmArgumentsAndWorkingDirectoryAreIgnored() {
		new MavenBuild("run-disable-fork").goals("spring-boot:run").execute((project) -> {
			assertThat(buildLog(project))
					.contains("I haz been run")
					.contains("Fork mode disabled, ignoring JVM argument(s) [-Dproperty1=value1 -Dproperty2 -Dfoo=bar]")
					.contains("Fork mode disabled, ignoring working directory configuration");
		});
	}
	
	@Test
	void whenEnvironmentVariablesAreConfiguredTheyAreAvailableToTheApplication() {
		new MavenBuild("run-envargs").goals("spring-boot:run").execute((project) -> assertThat(buildLog(project)).contains("I haz been run"));
	}
	
	@Test
	void whenExclusionsAreConfiguredExcludedDependenciesDoNotAppearOnTheClasspath() {
		new MavenBuild("run-exclude").goals("spring-boot:run").execute((project) -> assertThat(buildLog(project)).contains("I haz been run"));
	}
	
	@Test
	void whenSystemPropertiesAreConfiguredTheyAreAvailableToTheApplication() {
		new MavenBuild("run-jvm-system-props").goals("spring-boot:run").execute((project) -> assertThat(buildLog(project)).contains("I haz been run"));
	}
	
	@Test
	void whenJvmArgumentsAreConfiguredTheyAreAvailableToTheApplication() {
		new MavenBuild("run-jvmargs").goals("spring-boot:run").execute((project) -> assertThat(buildLog(project)).contains("I haz been run"));
	}
	
	@Test
	public void whenProfilesAreConfiguredTheyArePassedToTheApplication() {
		new MavenBuild("run-profiles").goals("spring-boot:run", "-X").execute((project) -> assertThat(buildLog(project)).contains("I haz been run with profile(s) 'foo,bar'"));		
	}
	
	@Test
	public void whenProfilesAreConfiguredAndForkingIsDisabledTheyArePassedToTheApplication() {
		new MavenBuild("run-profiles-fork-disabled").goals("spring-boot:run").execute((project) -> assertThat(buildLog(project)).contains("I haz been run with profile(s) 'foo,bar'"));		
	}
	
	@Test
	public void whenUseTestClasspathIsEnabledTheApplicationHasTestDependenciesOnItsClasspath() {
		new MavenBuild("run-use-test-classpath").goals("spring-boot:run").execute((project) -> assertThat(buildLog(project)).contains("I haz been run"));				
	}
	
	@Test
	public void whenAWorkingDirectoryIsConfiguredTheApplicationIsRunFromThatDirectory() {
		new MavenBuild("run-working-directory").goals("spring-boot:run").execute((project) -> assertThat(buildLog(project)).contains("I haz been run"));				
	}
	
	private String buildLog(File project) {
		return contentOf(new File(project, "target/build.log"));
	}
	
	private boolean isJava13OrLater() {
		for (Method method : String.class.getMethods()) {
			if (method.getName().equals("stripIndent")) {
				return true;
			}
		}
		return false;
	}

}
