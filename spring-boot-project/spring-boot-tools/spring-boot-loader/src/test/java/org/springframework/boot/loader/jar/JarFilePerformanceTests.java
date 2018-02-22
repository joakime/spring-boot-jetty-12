/*
 * Copyright 2012-2018 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.boot.loader.jar;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.jar.JarEntry;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

public class JarFilePerformanceTests {

	@ClassRule
	public static TemporaryFolder temp = new TemporaryFolder();

	private static File zip;

	@BeforeClass
	public static void createZip() throws IOException {
		zip = temp.newFile();
		try (ZipOutputStream out = new ZipOutputStream(new FileOutputStream(zip))) {
			for (int i = 0; i < 10000; i++) {
				out.putNextEntry(new ZipEntry("entry-" + i));
				out.write(new byte[] { 0, 1, 2, 3, 4, 5, 6, 7, 8, 9 });
				out.closeEntry();
			}
		}
	}

	@Test
	public void iterateWithBoot() throws Exception {
		try (JarFile jarFile = new JarFile(zip)) {
			iterateEntries(jarFile);
		}
	}

	@Test
	public void iterateWithJdk() throws Exception {
		try (java.util.jar.JarFile jarFile = new java.util.jar.JarFile(zip)) {
			iterateEntries(jarFile);
		}
	}

	@Test
	public void iterateWithNewBootJar() throws Exception {
		try (BootJarFile jarFile = new BootJarFile(zip)) {
			iterateEntries(jarFile);
		}
	}

	@Test
	public void iterateWithNewBootNestedJar() throws Exception {
		try (BootJarFile jarFile = new BootJarFile(new File(
				"/Users/awilkinson/dev/wilkinsona/spring-boot-jar-generator/jar-builder/build/libs/jar-builder.jar"))) {
			try (BootJarFile nestedJar = jarFile
					.getNestedEntry("BOOT-INF/lib/nested.jar")) {
				iterateEntries(nestedJar);
			}
		}
		System.out.println();
		try (JarFile jarFile = new JarFile(new File(
				"/Users/awilkinson/dev/wilkinsona/spring-boot-jar-generator/jar-builder/build/libs/jar-builder.jar"))) {
			JarFile nestedJar = jarFile
					.getNestedJarFile(jarFile.getJarEntry("BOOT-INF/lib/nested.jar"));
			iterateEntries(nestedJar);
		}
	}

	@Test
	public void iterateWithBootNestedJar() throws Exception {
		try (JarFile jarFile = new JarFile(new File(
				"/Users/awilkinson/dev/wilkinsona/spring-boot-jar-generator/jar-builder/build/libs/jar-builder.jar"))) {
			JarFile nestedJar = jarFile
					.getNestedJarFile(jarFile.getJarEntry("BOOT-INF/lib/nested.jar"));
			iterateEntries(nestedJar);
		}
	}

	private void iterateEntries(java.util.jar.JarFile jarFile) {
		for (int i = 0; i < 20; i++) {
			Enumeration<JarEntry> entries = jarFile.entries();
			List<String> names = new ArrayList<>();
			long start = System.currentTimeMillis();
			while (entries.hasMoreElements()) {
				JarEntry entry = entries.nextElement();
				names.add(entry.getName());
			}
			System.out.println(names.size() + " names in "
					+ (System.currentTimeMillis() - start) + "ms");
		}
	}

}
