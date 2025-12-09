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

tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile>().configureEach {
	compilerOptions {
		freeCompilerArgs.add("-Xjdk-release=21")
	}
}

tasks.withType<JavaCompile>().configureEach {
	options.compilerArgs.addAll(listOf("--enable-preview"))
}

// Compile C code to shared library
val compileNative by tasks.registering(Exec::class) {
	val srcDir = file("src/main/c")
	val outputDir = layout.buildDirectory.dir("native")

	inputs.dir(srcDir)
	outputs.dir(outputDir)

	doFirst {
		outputDir.get().asFile.mkdirs()
	}

	val os = System.getProperty("os.name").lowercase()
	val (libName, compilerFlags) = when {
		os.contains("linux") -> "libnative.so" to listOf("-shared", "-fPIC", "-O3", "-mavx2", "-mbmi")
		os.contains("mac") -> "libnative.dylib" to listOf("-shared", "-dynamiclib", "-O3", "-mavx2", "-mbmi")
		os.contains("windows") -> "native.dll" to listOf("-shared", "-O3", "-mavx2", "-mbmi")
		else -> throw GradleException("Unsupported OS: $os")
	}

	commandLine(
		listOf("cc") + compilerFlags + listOf(
			"-o", "${outputDir.get().asFile}/$libName",
			"${srcDir}/native.c"
		)
	)
}

tasks.withType<JavaExec>().configureEach {
	dependsOn(compileNative)
	workingDir = rootProject.projectDir
	jvmArgs("--enable-preview", "--enable-native-access=ALL-UNNAMED")
	systemProperty("java.library.path", layout.buildDirectory.dir("native").get().asFile.absolutePath)
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
