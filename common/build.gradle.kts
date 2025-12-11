plugins {
	kotlin("jvm") version "2.0.21"
	application
}

repositories {
	mavenCentral()
}

dependencies {
	testImplementation("io.kotest:kotest-runner-junit5:5.9.1")
	testImplementation("io.kotest:kotest-property:5.9.1")
}

tasks.withType<Test>().configureEach {
	useJUnitPlatform()
}

application {
	mainClass.set("RangeTreeKt")
}

tasks.register<JavaExec>("testRangeTree") {
	group = "verification"
	description = "Run RangeTree insert test"
	mainClass.set("RangeTreeKt")
	classpath = sourceSets["main"].runtimeClasspath
}
