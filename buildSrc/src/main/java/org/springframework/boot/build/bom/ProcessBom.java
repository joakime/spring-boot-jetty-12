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

package org.springframework.boot.build.bom;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.stream.Collectors;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathFactory;

import org.gradle.api.Task;
import org.gradle.api.internal.AbstractTask;
import org.gradle.api.tasks.TaskAction;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import org.springframework.boot.build.bom.Library.Exclusion;
import org.springframework.boot.build.bom.Library.Module;

/**
 * Temporary helper {@link Task} to keep the bom configuration in {@code build.gradle}
 * aligned with the equivalent configuration in {@code pom.xml} during the Maven to Gradle
 * migration.
 *
 * @author Andy Wilkinson
 */
public class ProcessBom extends AbstractTask {

	@TaskAction
	public void processBom() throws Exception {
		Document bom = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(getProject().file("pom.xml"));
		XPath xpath = XPathFactory.newInstance().newXPath();
		NodeList propertiesNodes = (NodeList) xpath.evaluate("//properties/*", bom, XPathConstants.NODESET);
		Map<String, String> versionProperties = new HashMap<>();
		System.out.println("bom {");
		for (int i = 0; i < propertiesNodes.getLength(); i++) {
			Node property = propertiesNodes.item(i);
			if (property.getNodeName().endsWith(".version")) {
				versionProperties.put(property.getNodeName(), property.getTextContent());
			}
		}
		Set<String> versions = new TreeSet<>();
		NodeList dependencyVersionNodes = (NodeList) xpath
				.evaluate("//dependencyManagement/dependencies/dependency/version", bom, XPathConstants.NODESET);
		for (int i = 0; i < dependencyVersionNodes.getLength(); i++) {
			versions.add(dependencyVersionNodes.item(i).getTextContent());
		}
		NodeList pluginVersionNodes = (NodeList) xpath.evaluate("//pluginManagement/plugins/plugin/version", bom,
				XPathConstants.NODESET);
		for (int i = 0; i < pluginVersionNodes.getLength(); i++) {
			versions.add(pluginVersionNodes.item(i).getTextContent());
		}
		Map<String, Group> groups = new TreeMap<>();
		for (String version : versions) {
			Map<String, Group> libraryGroups = new TreeMap<>();
			if ("${revision}".equals(version)) {
				System.out.println("    library('Spring Boot', '2.3.0.GRADLE-SNAPSHOT') {");
			}
			else {
				String versionProperty = version.substring(0, version.length() - 1).substring(2);
				System.out.println("    library('" + versionProperty.substring(0, versionProperty.length() - 8) + "', '"
						+ versionProperties.get(versionProperty) + "') {");
			}
			NodeList dependencies = (NodeList) xpath.evaluate(
					"//dependencyManagement/dependencies/dependency[version = '" + version + "']", bom,
					XPathConstants.NODESET);
			for (int i = 0; i < dependencies.getLength(); i++) {
				Node dependency = dependencies.item(i);
				String groupId = (String) xpath.evaluate("groupId/text()", dependency, XPathConstants.STRING);
				String artifactId = (String) xpath.evaluate("artifactId/text()", dependency, XPathConstants.STRING);
				String scope = (String) xpath.evaluate("scope/text()", dependency, XPathConstants.STRING);
				Group group = libraryGroups.computeIfAbsent(groupId, (key) -> new Group());
				if ("import".equals(scope)) {
					group.boms.add(artifactId);
				}
				else {
					NodeList exclusionNodes = (NodeList) xpath.evaluate("exclusions/exclusion", dependency,
							XPathConstants.NODESET);
					List<Exclusion> exclusions = new ArrayList<>();
					for (int j = 0; j < exclusionNodes.getLength(); j++) {
						Node exclusion = exclusionNodes.item(j);
						exclusions.add(new Exclusion(
								(String) xpath.evaluate("groupId/text()", exclusion, XPathConstants.STRING),
								(String) xpath.evaluate("artifactId/text()", exclusion, XPathConstants.STRING)));
					}
					group.modules.add(new Module(artifactId, exclusions));
				}
			}
			NodeList plugins = (NodeList) xpath.evaluate(
					"//pluginManagement/plugins/plugin[version = '" + version + "']", bom, XPathConstants.NODESET);
			for (int i = 0; i < plugins.getLength(); i++) {
				Node plugin = plugins.item(i);
				String groupId = (String) xpath.evaluate("groupId/text()", plugin, XPathConstants.STRING);
				String artifactId = (String) xpath.evaluate("artifactId/text()", plugin, XPathConstants.STRING);
				Group group = libraryGroups.computeIfAbsent(groupId, (key) -> new Group());
				group.plugins.add(artifactId);
			}
			for (Entry<String, Group> identifiedGroup : libraryGroups.entrySet()) {
				System.out.println("        group('" + identifiedGroup.getKey() + "') {");
				Group group = identifiedGroup.getValue();
				if (!group.modules.isEmpty()) {
					System.out.println("            modules = [");
					System.out.println(String.join(",\n", group.modules.stream().map((module) -> {
						String formatted = "                '" + module.getName() + "'";
						if (!module.getExclusions().isEmpty()) {
							formatted += " {\n";
							for (Exclusion exclusion : module.getExclusions()) {
								formatted += "                    exclude group: '" + exclusion.getGroupId()
										+ "', module: '" + exclusion.getArtifactId() + "'\n";
							}
							formatted += "                }";
						}
						return formatted;
					}).collect(Collectors.toList())));
					System.out.println("            ]");
				}
				if (!group.boms.isEmpty()) {
					System.out.println("            imports = [");
					System.out.println(String.join(",\n", group.boms.stream()
							.map((name) -> "                '" + name + "'").collect(Collectors.toList())));
					System.out.println("            ]");
				}
				if (!group.plugins.isEmpty()) {
					System.out.println("            plugins = [");
					System.out.println(String.join(",\n", group.plugins.stream()
							.map((name) -> "                '" + name + "'").collect(Collectors.toList())));
					System.out.println("            ]");
				}
				System.out.println("        }");
			}
			System.out.println("    }");
			groups.putAll(libraryGroups);
		}
		System.out.println("}");
	}

	private static class Group {

		private final List<Module> modules = new ArrayList<>();

		private final List<String> boms = new ArrayList<>();

		private final List<String> plugins = new ArrayList<>();

	}

}
