import groovy.util.XmlSlurper
import groovy.io.FileType

projectPaths = [:]

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
	return "api"
}

def processStarter(File starter) {
	def pom = new XmlSlurper().parse(new File(starter, "pom.xml"))

	def allDependencies = [:] as TreeMap

	pom.dependencies.dependency.each { dependency ->
		def configuration = getConfigurationForDependency(dependency)
		def dependencies = allDependencies.computeIfAbsent(configuration) { [] }
		if ('org.springframework.boot' == dependency.groupId.text()) {
			dependencies.add("project('${findProject(dependency.artifactId.text())}')")
		}
		else {
			dependencies.add("'${dependency.groupId.text()}:${dependency.artifactId.text()}'")
		}
	}

	PrintWriter writer = new PrintWriter(new FileWriter(new File(starter, 'build.gradle')))
	writer.println "plugins {"
	writer.println "\tid 'java-library'"
	writer.println "\tid 'maven-publish'"
	writer.println "\tid 'org.springframework.boot.conventions'"
	writer.println "}"
	writer.println ""
	writer.println "description = \"${pom.description.text().replace('\n', ' ')}\""
	writer.println ""
	writer.println "dependencies {"
	allDependencies.each {configuration, dependencies ->
		if (['api', 'annotationProcessor', 'implementation', 'optional'].contains(configuration)) {
			writer.println "\t$configuration enforcedPlatform(project(':spring-boot-project:spring-boot-dependencies'))"
		}
		dependencies.each { dependency ->
			writer.println "\t$configuration $dependency"
		}
	}
	writer.println "}"
	writer.flush()
}

def findProject(String artifactId) {
	String path = projectPaths[artifactId]
	if (path) {
		return path
	}
	File match
	new File(".").eachFileRecurse(FileType.DIRECTORIES) { candidate ->
		if (candidate.getName() == artifactId) {
			match = candidate
		}
	}
	if (!match) {
		throw new IllegalStateException("Project not found for ${artifactId}")
	}
	path = match.getPath().replace(".", "").replace("/", ":")
	projectPaths[artifactId] = path
	return path
}

new File(args[0]).listFiles().findAll { it.name.startsWith('spring-boot-starter') }.each {
	println "include 'spring-boot-project:spring-boot-starters:${it.name}'"
	processStarter(it)
}