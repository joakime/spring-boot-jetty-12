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

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.Properties;
import java.util.function.Consumer;

import org.assertj.core.api.AbstractMapAssert;
import org.assertj.core.api.AssertProvider;
import org.junit.jupiter.api.Test;

/**
 * Integration tests for the Maven plugin's build info support.
 * 
 * @author Andy Wilkinson
 */
public class BuildInfoIntegrationTests {
	
	@Test
	public void buildInfoPropertiesAreGenerated() {
		new MavenBuild("build-info").execute(buildInfo((buildInfo) -> {
			assertThat(buildInfo)
					.hasBuildGroup("org.springframework.boot.maven.it")
					.hasBuildArtifact("build-info")
					.hasBuildName("Generate build info")
					.hasBuildVersion("0.0.1.BUILD-SNAPSHOT")
					.containsBuildTime();
		}));
	}
	
	@Test
	public void generatedBuildInfoIncludesAdditionalProperties() {
		new MavenBuild("build-info-additional-properties").execute(buildInfo((buildInfo) -> {
			assertThat(buildInfo)
					.hasBuildGroup("org.springframework.boot.maven.it")
					.hasBuildArtifact("build-info-additional-properties")
					.hasBuildName("Generate build info with additional properties")
					.hasBuildVersion("0.0.1.BUILD-SNAPSHOT")
					.containsBuildTime()
					.containsEntry("build.foo", "bar")
					.containsEntry("build.encoding", "UTF-8")
					.containsEntry("build.java.source", "1.8");
		}));
	}

	@Test
	public void generatedBuildInfoUsesCustomBuildTime() {
		new MavenBuild("build-info-custom-build-time").execute(buildInfo((buildInfo) -> {
			assertThat(buildInfo)
					.hasBuildGroup("org.springframework.boot.maven.it")
					.hasBuildArtifact("build-info-custom-build-time")
					.hasBuildName("Generate build info with custom build time")
					.hasBuildVersion("0.0.1.BUILD-SNAPSHOT")
					.hasBuildTime("2019-07-08T08:00:00Z");
		}));
	}
	
	@Test
	public void buildInfoPropertiesAreGeneratedToCustomOutputLocation() {
		new MavenBuild("build-info-custom-file").execute(buildInfo("target/build.info", (buildInfo) -> {
			assertThat(buildInfo)
					.hasBuildGroup("org.springframework.boot.maven.it")
					.hasBuildArtifact("build-info-custom-file")
					.hasBuildName("Generate custom build info")
					.hasBuildVersion("0.0.1.BUILD-SNAPSHOT")
					.containsBuildTime();
		}));
	}
	
	@Test
	public void whenBuildTimeIsDisabledIfDoesNotAppearInGeneratedBuildInfo() {
		new MavenBuild("build-info-disable-build-time").execute(buildInfo((buildInfo) -> {
			assertThat(buildInfo)
					.hasBuildGroup("org.springframework.boot.maven.it")
					.hasBuildArtifact("build-info-disable-build-time")
					.hasBuildName("Generate build info with disabled build time")
					.hasBuildVersion("0.0.1.BUILD-SNAPSHOT")
					.doesNotContainBuildTime();
		}));
	}
	
	private Consumer<File> buildInfo(Consumer<AssertProvider<BuildInfoAssert>> buildInfo) {
		return buildInfo("target/classes/META-INF/build-info.properties", buildInfo);
	}
	
	private Consumer<File> buildInfo(String location, Consumer<AssertProvider<BuildInfoAssert>> buildInfo) {
		return (project) -> buildInfo.accept((buildInfo(project, location)));
	}
	
	private AssertProvider<BuildInfoAssert> buildInfo(File project, String buildInfo) {
		return new AssertProvider<BuildInfoAssert>() {

			@Override
			@Deprecated
			public BuildInfoAssert assertThat() {
				return new BuildInfoAssert(new File(project, buildInfo));
			}
			
		};
	}
	
	private static class BuildInfoAssert extends AbstractMapAssert<BuildInfoAssert, Properties, Object, Object> {
		
		public BuildInfoAssert(File actual) {
			super(loadProperties(actual), BuildInfoAssert.class);
		}
		
		private static Properties loadProperties(File file) {
			try (FileReader reader = new FileReader(file)) {
				Properties properties = new Properties();
				properties.load(reader);
				return properties;
			}
			catch (IOException ex) {
				throw new RuntimeException(ex);
			}
		}
		
		public BuildInfoAssert hasBuildGroup(String expected) {
			return containsEntry("build.group",  expected);
		}
		
		public BuildInfoAssert hasBuildArtifact(String expected) {
			return containsEntry("build.artifact",  expected);
		}
		
		public BuildInfoAssert hasBuildName(String expected) {
			return containsEntry("build.name",  expected);
		}
		
		public BuildInfoAssert hasBuildVersion(String expected) {
			return containsEntry("build.version",  expected);
		}
		
		public BuildInfoAssert containsBuildTime() {
			return containsKey("build.time");
		}
		
		public BuildInfoAssert doesNotContainBuildTime() {
			return doesNotContainKey("build.time");
		}
		
		public BuildInfoAssert hasBuildTime(String expected) {
			return containsEntry("build.time",  expected);
		}
		
	}
	
}
