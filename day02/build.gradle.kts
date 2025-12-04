plugins {
	kotlin("jvm") version "2.0.21"
}

repositories {
	mavenCentral()
}

tasks.register<JavaExec>("testCasePartOne") {
	dependsOn("classes")
	mainClass.set("TestCasePartOne")
	classpath = sourceSets["main"].runtimeClasspath
}

tasks.register<JavaExec>("sumInvalidIdsPartOne") {
	dependsOn("classes")
	mainClass.set("SumInvalidIdsPartOne")
	classpath = sourceSets["main"].runtimeClasspath
}

tasks.register<JavaExec>("testCasePartTwo") {
	dependsOn("classes")
	mainClass.set("TestCasePartTwo")
	classpath = sourceSets["main"].runtimeClasspath
}

tasks.register<JavaExec>("sumInvalidIdsPartTwo") {
	dependsOn("classes")
	mainClass.set("SumInvalidIdsPartTwo")
	classpath = sourceSets["main"].runtimeClasspath
}