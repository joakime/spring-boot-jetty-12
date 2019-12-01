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

package org.springframework.boot.build.test;

import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.gradle.api.plugins.JavaPlugin;
import org.gradle.api.plugins.JavaPluginConvention;
import org.gradle.api.tasks.SourceSet;
import org.gradle.api.tasks.SourceSetContainer;
import org.gradle.api.tasks.testing.Test;
import org.gradle.language.base.plugins.LifecycleBasePlugin;

/**
 * A {@Plugin} to configure integration testing support in a {@link Project}.
 *
 * @author Andy Wilkinson
 */
public class IntegrationTestPlugin implements Plugin<Project> {

	@Override
	public void apply(Project project) {
		project.getPlugins().withType(JavaPlugin.class, (javaPlugin) -> this.configureIntegrationTesting(project));
	}

	private void configureIntegrationTesting(Project project) {
		SourceSet intTestSourceSet = createSourceSet(project);
		Test intTest = createTestTask(project, intTestSourceSet);
		project.getTasks().getByName("check").dependsOn(intTest);
	}

	private SourceSet createSourceSet(Project project) {
		SourceSetContainer sourceSets = project.getConvention().getPlugin(JavaPluginConvention.class).getSourceSets();
		SourceSet intTestSourceSet = sourceSets.create("intTest");
		SourceSet main = sourceSets.getByName("main");
		intTestSourceSet.setCompileClasspath(intTestSourceSet.getCompileClasspath().plus(main.getOutput()));
		intTestSourceSet.setRuntimeClasspath(intTestSourceSet.getRuntimeClasspath().plus(main.getOutput()));
		return intTestSourceSet;
	}

	private Test createTestTask(Project project, SourceSet intTestSourceSet) {
		Test intTest = project.getTasks().create("intTest", Test.class);
		intTest.setGroup(LifecycleBasePlugin.VERIFICATION_GROUP);
		intTest.setDescription("Runs integration tests.");
		intTest.setTestClassesDirs(intTestSourceSet.getOutput().getClassesDirs());
		intTest.setClasspath(intTestSourceSet.getRuntimeClasspath());
		intTest.shouldRunAfter(JavaPlugin.TEST_TASK_NAME);
		return intTest;
	}

}
