/*
 * Copyright 2012-2022 the original author or authors.
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

package org.springframework.boot.gradle.tasks.aot;

import java.io.File;
import java.lang.reflect.InvocationTargetException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import org.gradle.api.DefaultTask;
import org.gradle.api.GradleException;
import org.gradle.api.file.DirectoryProperty;
import org.gradle.api.file.FileCollection;
import org.gradle.api.provider.Property;
import org.gradle.api.tasks.Classpath;
import org.gradle.api.tasks.Input;
import org.gradle.api.tasks.OutputDirectory;
import org.gradle.api.tasks.TaskAction;

/**
 * @author awilkinson
 */
public class GenerateAotSources extends DefaultTask {

	private final Property<String> mainClass;

	private final DirectoryProperty sourcesDir;

	private final DirectoryProperty resourcesDir;

	private FileCollection classpath;

	public GenerateAotSources() {
		this.mainClass = getProject().getObjects().property(String.class);
		this.sourcesDir = getProject().getObjects().directoryProperty();
		this.resourcesDir = getProject().getObjects().directoryProperty();
	}

	@Input
	public Property<String> getMainClass() {
		return this.mainClass;
	}

	@Classpath
	public FileCollection getClasspath() {
		return this.classpath;
	}

	public void setClasspath(FileCollection classpath) {
		this.classpath = classpath;
	}

	@OutputDirectory
	public DirectoryProperty getSourcesDir() {
		return this.sourcesDir;
	}

	@OutputDirectory
	public DirectoryProperty getResourcesDir() {
		return this.resourcesDir;
	}

	@TaskAction
	void generateAotSources() throws Exception {
		List<URL> urls = this.classpath.getFiles().stream().map(this::toURL)
				.collect(Collectors.toCollection(ArrayList::new));
		urls.add(AotInvoker.class.getProtectionDomain().getCodeSource().getLocation());
		URLClassLoader classLoader = new URLClassLoader(urls.toArray(new URL[0]), ClassLoader.getPlatformClassLoader());
		ClassLoader previousTccl = Thread.currentThread().getContextClassLoader();
		try {
			Thread.currentThread().setContextClassLoader(classLoader);
			Class<?> mainClass = Class.forName(getMainClass().get(), true, classLoader);
			Class<?> aotProcessorClass = Class.forName("org.springframework.boot.AotProcessor", true, classLoader);
			Object aotProcessor = aotProcessorClass.getConstructor(Class.class, Path.class, Path.class).newInstance(
					mainClass, this.sourcesDir.get().getAsFile().toPath(),
					this.resourcesDir.get().getAsFile().toPath());
			aotProcessorClass.getMethod("process").invoke(aotProcessor);
		}
		catch (InvocationTargetException ex) {
			throw new GradleException(ex.getCause().getMessage(), ex.getCause());
		}
		finally {
			Thread.currentThread().setContextClassLoader(previousTccl);
		}
	}

	private URL toURL(File file) {
		try {
			return file.toURI().toURL();
		}
		catch (MalformedURLException ex) {
			throw new RuntimeException(ex);
		}
	}

}
