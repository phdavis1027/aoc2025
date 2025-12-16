plugins {
	kotlin("jvm") version "2.0.21"
}

repositories {
	mavenCentral()
}

dependencies {
	implementation(project(":common"))
}

kotlin {
	jvmToolchain(21)
}

tasks.withType<JavaExec>().configureEach {
	workingDir = rootProject.projectDir
}

tasks.register<JavaExec>("testCasePartOne") {
	dependsOn("classes")
	mainClass.set("TestCasePartOne")
	classpath = sourceSets["main"].runtimeClasspath
	standardOutput = System.out
	errorOutput = System.err
}

tasks.register<JavaExec>("solvePartOne") {
	dependsOn("classes")
	mainClass.set("SolvePartOne")
	classpath = sourceSets["main"].runtimeClasspath
	standardOutput = System.out
	errorOutput = System.err
}

tasks.register<JavaExec>("testCasePartTwo") {
	dependsOn("classes")
	mainClass.set("TestCasePartTwo")
	classpath = sourceSets["main"].runtimeClasspath
	standardOutput = System.out
	errorOutput = System.err
}

tasks.register<JavaExec>("solvePartTwo") {
	dependsOn("classes")
	mainClass.set("SolvePartTwo")
	classpath = sourceSets["main"].runtimeClasspath
	standardOutput = System.out
	errorOutput = System.err
}
