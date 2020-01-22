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

import java.io.File;
import java.util.Collections;
import java.util.concurrent.Callable;
import java.util.function.BiConsumer;

import org.gradle.api.Action;
import org.gradle.api.file.CopySpec;
import org.gradle.api.file.FileCollection;
import org.gradle.api.file.FileCopyDetails;
import org.gradle.api.file.FileTreeElement;
import org.gradle.api.internal.file.copy.CopyAction;
import org.gradle.api.java.archives.Attributes;
import org.gradle.api.specs.Spec;
import org.gradle.api.tasks.Input;
import org.gradle.api.tasks.Internal;
import org.gradle.api.tasks.Optional;
import org.gradle.api.tasks.bundling.Jar;

import org.springframework.boot.loader.tools.Layer;
import org.springframework.boot.loader.tools.Layers;
import org.springframework.boot.loader.tools.Library;

/**
 * A custom {@link Jar} task that produces a Spring Boot executable jar.
 *
 * @author Andy Wilkinson
 * @since 2.0.0
 */
public class BootJar extends Jar implements BootArchive {

	private final BootArchiveSupport support = new BootArchiveSupport("org.springframework.boot.loader.JarLauncher",
			this::resolveZipCompression);

	private final CopySpec bootInf;

	private String mainClassName;

	private FileCollection classpath;

	private LayerConfiguration layerConfiguration;

	private static final String BOOT_INF_LAYERS = "BOOT-INF/layers/";

	/**
	 * Creates a new {@code BootJar} task.
	 */
	public BootJar() {
		this.bootInf = getProject().copySpec().into("BOOT-INF");
		getMainSpec().with(this.bootInf);
		this.bootInf.with(layers(File::isDirectory, this::applyApplicationLayer));
		this.bootInf.with(layers(File::isFile, this::applyLibraryLayer));
		this.bootInf.into("classes", classpathFiles(File::isDirectory));
		this.bootInf.into("lib", classpathFiles(File::isFile));
		this.bootInf.filesMatching("module-info.class",
				(details) -> details.setRelativePath(details.getRelativeSourcePath()));
		getRootSpec().eachFile((details) -> {
			String pathString = details.getRelativePath().getPathString();
			if (pathString.startsWith("BOOT-INF/lib/") && !this.support.isZip(details.getFile())) {
				details.exclude();
			}
		});
	}

	private CopySpec layers(Spec<File> filter, BiConsumer<FileCopyDetails, Layers> consumer) {
		CopySpec layers = getProject().copySpec();
		layers.from((Callable<Iterable<File>>) () -> {
			if (BootJar.this.layerConfiguration != null && BootJar.this.classpath != null) {
				return this.classpath.filter(filter);
			}
			return Collections.emptyList();
		}).eachFile(applyLayer(consumer)).setIncludeEmptyDirs(false);
		return layers;
	}

	private Action<FileCopyDetails> applyLayer(BiConsumer<FileCopyDetails, Layers> consumer) {
		return (details) -> {
			LayerConfiguration layerConfiguration = BootJar.this.layerConfiguration;
			Layers layers = layerConfiguration.getLayers();
			consumer.accept(details, layers);
		};
	}

	private void applyApplicationLayer(FileCopyDetails details, Layers layers) {
		Layer layer = layers.getLayer(details.getSourcePath());
		details.setPath(BOOT_INF_LAYERS + layer + "/classes/" + details.getSourcePath());
	}

	private void applyLibraryLayer(FileCopyDetails details, Layers layers) {
		Layer layer = layers.getLayer(new Library(details.getFile(), null));
		details.setPath(BOOT_INF_LAYERS + layer + "/lib/" + details.getSourcePath());
	}

	private Action<CopySpec> classpathFiles(Spec<File> filter) {
		return (copySpec) -> copySpec.from((Callable<Iterable<File>>) () -> {
			if (BootJar.this.layerConfiguration != null) {
				return Collections.emptyList();
			}
			return (BootJar.this.classpath != null) ? BootJar.this.classpath.filter(filter) : Collections.emptyList();
		});
	}

	@Override
	public void copy() {
		this.support.configureManifest(this, getMainClassName(), "BOOT-INF/classes/", "BOOT-INF/lib/");
		Attributes attributes = this.getManifest().getAttributes();
		if (this.layerConfiguration != null) {
			attributes.remove("Spring-Boot-Classes");
			attributes.remove("Spring-Boot-Lib");
			attributes.putIfAbsent("Spring-Boot-Layers-Index", "BOOT-INF/layers.idx");
		}
		super.copy();
	}

	@Override
	protected CopyAction createCopyAction() {
		return this.support.createCopyAction(this);
	}

	@Override
	public String getMainClassName() {
		if (this.mainClassName == null) {
			String manifestStartClass = (String) getManifest().getAttributes().get("Start-Class");
			if (manifestStartClass != null) {
				setMainClassName(manifestStartClass);
			}
		}
		return this.mainClassName;
	}

	@Override
	public void setMainClassName(String mainClassName) {
		this.mainClassName = mainClassName;
	}

	@Override
	public void requiresUnpack(String... patterns) {
		this.support.requiresUnpack(patterns);
	}

	@Override
	public void requiresUnpack(Spec<FileTreeElement> spec) {
		this.support.requiresUnpack(spec);
	}

	@Override
	public LaunchScriptConfiguration getLaunchScript() {
		return this.support.getLaunchScript();
	}

	@Override
	public void launchScript() {
		enableLaunchScriptIfNecessary();
	}

	@Override
	public void launchScript(Action<LaunchScriptConfiguration> action) {
		action.execute(enableLaunchScriptIfNecessary());
	}

	/**
	 * Configures the archive to have layers.
	 */
	public void layered() {
		this.layerConfiguration = new LayerConfiguration();
	}

	@Input
	@Optional
	LayerConfiguration getLayerConfiguration() {
		return this.layerConfiguration;
	}

	@Override
	public FileCollection getClasspath() {
		return this.classpath;
	}

	@Override
	public void classpath(Object... classpath) {
		FileCollection existingClasspath = this.classpath;
		this.classpath = getProject().files((existingClasspath != null) ? existingClasspath : Collections.emptyList(),
				classpath);
	}

	@Override
	public void setClasspath(Object classpath) {
		this.classpath = getProject().files(classpath);
	}

	@Override
	public void setClasspath(FileCollection classpath) {
		this.classpath = getProject().files(classpath);
	}

	@Override
	public boolean isExcludeDevtools() {
		return this.support.isExcludeDevtools();
	}

	@Override
	public void setExcludeDevtools(boolean excludeDevtools) {
		this.support.setExcludeDevtools(excludeDevtools);
	}

	/**
	 * Returns a {@code CopySpec} that can be used to add content to the {@code BOOT-INF}
	 * directory of the jar.
	 * @return a {@code CopySpec} for {@code BOOT-INF}
	 * @since 2.0.3
	 */
	@Internal
	public CopySpec getBootInf() {
		CopySpec child = getProject().copySpec();
		this.bootInf.with(child);
		return child;
	}

	/**
	 * Calls the given {@code action} to add content to the {@code BOOT-INF} directory of
	 * the jar.
	 * @param action the {@code Action} to call
	 * @return the {@code CopySpec} for {@code BOOT-INF} that was passed to the
	 * {@code Action}
	 * @since 2.0.3
	 */
	public CopySpec bootInf(Action<CopySpec> action) {
		CopySpec bootInf = getBootInf();
		action.execute(bootInf);
		return bootInf;
	}

	/**
	 * Returns the {@link ZipCompression} that should be used when adding the file
	 * represented by the given {@code details} to the jar.
	 * <p>
	 * By default, any file in {@code BOOT-INF/lib/} is stored and all other files are
	 * deflated.
	 * @param details the details
	 * @return the compression to use
	 */
	protected ZipCompression resolveZipCompression(FileCopyDetails details) {
		if (details.getRelativePath().getPathString().startsWith("BOOT-INF/lib/")) {
			return ZipCompression.STORED;
		}
		return ZipCompression.DEFLATED;
	}

	private LaunchScriptConfiguration enableLaunchScriptIfNecessary() {
		LaunchScriptConfiguration launchScript = this.support.getLaunchScript();
		if (launchScript == null) {
			launchScript = new LaunchScriptConfiguration(this);
			this.support.setLaunchScript(launchScript);
		}
		return launchScript;
	}

}
