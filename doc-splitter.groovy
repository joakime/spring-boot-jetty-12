@Grab("org.apache.commons:commons-lang3:3.10")
import org.apache.commons.lang3.StringUtils

input = args[0]
def previous = null
def directory = new File(input[0 ..< input.lastIndexOf('.')])
def index = new File(directory, "index.adoc")
index.parentFile.mkdirs()
def output = index
def lines = new File(input).readLines()
def sectionIds = []
for (int i = 0; i < lines.size(); i++) {
	def line = lines.get(i);
	if (line.startsWith("[[") && line.endsWith("]]") && lines.get(i + 1).startsWith("== ")) {
		sectionIds << "${line[2 ..< -2]}"
	}
}

def commonSectionPrefix = StringUtils.getCommonPrefix(sectionIds as String[])

for (int i = 0; i < lines.size(); i++) {
	def line = lines.get(i);
	if (line.startsWith("[[") && line.endsWith("]]") && lines.get(i + 1).startsWith("== ")) {
		output = new File(directory, "${line[commonSectionPrefix.length() + 2 ..< -2]}.adoc")
		output.parentFile.mkdirs()	
		index << "include::${output.name}\n"
	}
	output << "$line\n"
}
