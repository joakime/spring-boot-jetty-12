import groovy.util.XmlSlurper
import groovy.io.FileType

def getConfigurationForDependency(def dependency) {
	def annotationProcessors = ['spring-boot-configuration-processor', 'spring-boot-autoconfigure-processor']
	if (annotationProcessors.contains(dependency.artifactId.text())) {
		return "annotationProcessor"
	}
	else if (dependency.optional.text()) {
		return "optional"
	}
	else if ("test".equals(dependency.scope.text())) {
		return "testImplementation"
	}
	else if ("provided".equals(dependency.scope.text())) {
		return "compileOnly"
	}
	return "implementation"
}

def pom = new XmlSlurper().parse(new File(args[0], "pom.xml"))

def allDependencies = [:] as TreeMap

pom.dependencies.dependency.each { dependency ->
	println dependency
	def configuration = getConfigurationForDependency(dependency)
	def dependencies = allDependencies.computeIfAbsent(configuration) { [] }
	if ('org.springframework.boot' == dependency.groupId.text()) {
		dependencies.add("project('${findProject(dependency.artifactId.text())}')")
	}
	else {
		dependencies.add("'${dependency.groupId.text()}:${dependency.artifactId.text()}'")
	}
}

PrintWriter writer = new PrintWriter(new FileWriter(new File(args[0], 'build.gradle')))
writer.println "plugins {"
writer.println "    id 'java'"
writer.println "}"
writer.println ""
writer.println "description = '${pom.description}'"
writer.println ""
writer.println "dependencies {"
allDependencies.each {configuration, dependencies ->
	if (['annotationProcessor', 'implementation', 'optional'].contains(configuration)) {
		writer.println "    $configuration enforcedPlatform(project(':spring-boot-project:spring-boot-parent'))"
	}
	dependencies.each { dependency ->
		writer.println "    $configuration $dependency"
	}
}
writer.println "}"
writer.close()

def findProject(String artifactId) {
	File match
	new File(".").eachFileRecurse(FileType.DIRECTORIES) { candidate ->
		if (candidate.getName() == artifactId) {
			match = candidate
		}
	}
	if (!match) {
		throw new IllegalStateException("Project not found for ${artifactId}")
	}
	return match.getPath().replace(".", "").replace("/", ":")
}