plugins {
	kotlin("jvm") version "2.0.21"
	application
}

repositories {
	mavenCentral()
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
