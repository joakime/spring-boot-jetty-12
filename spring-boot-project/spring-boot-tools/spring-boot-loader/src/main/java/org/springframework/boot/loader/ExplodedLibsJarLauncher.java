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

package org.springframework.boot.loader;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.net.URLClassLoader;
import java.security.CodeSource;
import java.security.ProtectionDomain;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

/**
 * @author awilkinson
 */
public class ExplodedLibsJarLauncher {

	private final File jar;

	private final JarFile jarFile;

	private ExplodedLibsJarLauncher() throws Exception {
		ProtectionDomain protectionDomain = getClass().getProtectionDomain();
		CodeSource codeSource = protectionDomain.getCodeSource();
		URI location = (codeSource != null) ? codeSource.getLocation().toURI() : null;
		String path = (location != null) ? location.getSchemeSpecificPart() : null;
		if (path == null) {
			throw new IllegalStateException("Unable to determine code source archive");
		}
		this.jar = new File(path);
		this.jarFile = new JarFile(new File(path));
	}

	protected boolean isNestedArchive(JarEntry entry) {
		if (!entry.isDirectory()) {
			return false;
		}
		String name = entry.getName();
		if (name.equals("BOOT-INF/classes/")) {
			return true;
		}
		return name.startsWith("BOOT-INF/lib/") && name.indexOf('/', 13) == name.lastIndexOf('/');
	}

	public static void main(String[] args) throws Exception {
		new ExplodedLibsJarLauncher().launch(args);
	}

	private void launch(String[] args) throws Exception {
		ClassLoader classLoader = createClassLoader();
		String launchClass = getMainClass();
		launch(args, launchClass, classLoader);
	}

	private ClassLoader createClassLoader() throws MalformedURLException {
		Enumeration<JarEntry> entries = this.jarFile.entries();
		List<URL> urls = new ArrayList<>();
		while (entries.hasMoreElements()) {
			JarEntry entry = entries.nextElement();
			if (isNestedArchive(entry)) {
				urls.add(new URL("jar:file:" + this.jar + "!/" + entry.getName()));
			}
		}
		return new URLClassLoader(urls.toArray(new URL[0]));
	}

	private String getMainClass() throws IOException {
		return this.jarFile.getManifest().getMainAttributes().getValue("Start-Class");
	}

	private void launch(String[] args, String launchClass, ClassLoader classLoader) throws Exception {
		Thread.currentThread().setContextClassLoader(classLoader);
		createMainMethodRunner(launchClass, args, classLoader).run();
	}

	private MainMethodRunner createMainMethodRunner(String mainClass, String[] args, ClassLoader classLoader) {
		return new MainMethodRunner(mainClass, args);
	}

}
