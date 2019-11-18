package org.springframework.boot.maven;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.contentOf;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;
import java.util.function.Consumer;

import org.apache.maven.shared.invoker.DefaultInvocationRequest;
import org.apache.maven.shared.invoker.DefaultInvoker;
import org.apache.maven.shared.invoker.InvocationOutputHandler;
import org.apache.maven.shared.invoker.InvocationRequest;
import org.apache.maven.shared.invoker.InvocationResult;
import org.apache.maven.shared.invoker.Invoker;
import org.apache.maven.shared.invoker.MavenInvocationException;

/**
 * Helper class for executing a Maven build.
 * 
 * @author Andy Wilkinson
 *
 */
class MavenBuild {
	
	private final File projectDir;
	
	private final File temp;
	
	private final Map<String, String> pomReplacements = new HashMap<>();
	
	private final List<String> goals = new ArrayList<>();
	
	public MavenBuild(String project) {
		this.projectDir = new File("src/it/" + project);
		try {
			this.temp = Files.createTempDirectory("maven-build").toFile().getCanonicalFile();
		} catch (IOException ex) {
			throw new RuntimeException(ex);
		}
		pomReplacements.put("java.version", "1.8");
		pomReplacements.put("project.groupId", "org.springframework.boot");
		pomReplacements.put("project.artifactId", "spring-boot-maven-plugin");
		pomReplacements.put("project.version", determineVersion());
		pomReplacements.put("log4j2.version", "2.12.1");
		pomReplacements.put("maven-jar-plugin.version", "3.2.0");
		pomReplacements.put("maven-war-plugin.version", "3.2.3");
		pomReplacements.put("build-helper-maven-plugin.version", "3.0.0");
		pomReplacements.put("spring-framework.version", "5.2.1.RELEASE");
		pomReplacements.put("jakarta-servlet.version", "4.0.2");
		pomReplacements.put("kotlin.version", "1.3.60");
	}
	
	public MavenBuild goals(String... goals) {
		this.goals.addAll(Arrays.asList(goals));
		return this;
	}
	
	public void execute(Consumer<File> callback) {
		Invoker invoker = new DefaultInvoker();
		invoker.setMavenHome(new File("build/maven-binaries/apache-maven-3.6.2"));
		InvocationRequest request = new DefaultInvocationRequest();
		try {
			Path destination = temp.toPath();
			Path source = this.projectDir.toPath();
			Files.walkFileTree(source, new SimpleFileVisitor<Path>() {
				
				@Override
				public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException {
					Files.createDirectories(destination.resolve(source.relativize(dir)));
					return FileVisitResult.CONTINUE;
				}
				
				@Override
				public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
					if (file.toFile().getName().equals("pom.xml")) {
						String pomXml = new String(Files.readAllBytes(file), StandardCharsets.UTF_8);
						for (Entry<String, String> replacement: pomReplacements.entrySet()) {					
							pomXml = pomXml.replace("@" + replacement.getKey() + "@", replacement.getValue());
						}
						Files.write(destination.resolve(source.relativize(file)), pomXml.getBytes(StandardCharsets.UTF_8), StandardOpenOption.CREATE_NEW);
					}
					else {
						Files.copy(file, destination.resolve(source.relativize(file)), StandardCopyOption.REPLACE_EXISTING);
					}
					return FileVisitResult.CONTINUE;
				}
				
			});
			String settingsXml = new String(Files.readAllBytes(Paths.get("src", "it", "settings.xml")), StandardCharsets.UTF_8).replace("@localRepositoryUrl@", new File("build/local-maven-repository").toURI().toURL().toString());
			Files.write(destination.resolve("settings.xml"), settingsXml.getBytes(StandardCharsets.UTF_8), StandardOpenOption.CREATE_NEW);
			request.setBaseDirectory(temp);
			request.setGoals(this.goals.isEmpty() ? Collections.singletonList("package") : this.goals);
			request.setUserSettingsFile(new File(temp, "settings.xml"));
			request.setUpdateSnapshots(true);
			request.setBatchMode(true);
			File target = new File(temp, "target");
			target.mkdirs();
			File buildLogFile = new File(target, "build.log");
			try (PrintWriter buildLog = new PrintWriter(new FileWriter(buildLogFile))) {
				request.setOutputHandler(new InvocationOutputHandler() {
					
					@Override
					public void consumeLine(String line) {
						buildLog.println(line);	
						buildLog.flush();
					}
					
				});
				try {
					InvocationResult result = invoker.execute(request);
					assertThat(result.getExitCode()).as(contentOf(buildLogFile)).isEqualTo(0);
				} catch (MavenInvocationException ex) {
					throw new RuntimeException(ex);
				}
			}
			callback.accept(this.temp);
		} catch (Exception ex) {
			throw new RuntimeException(ex);
		}
	}
	
	private String determineVersion() {
		File gradleProperties = new File("gradle.properties").getAbsoluteFile();
		while (!gradleProperties.isFile()) {
			gradleProperties = new File(gradleProperties.getParentFile().getParentFile(), "gradle.properties");
		}
		Properties properties = new Properties();
		try (Reader reader = new FileReader(gradleProperties)) {
			properties.load(reader);
			return properties.getProperty("version");
		}
		catch (IOException ex) {
			throw new RuntimeException(ex);
		}
	}

}
