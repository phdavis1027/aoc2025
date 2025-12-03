import org.gradle.api.Project

/**
 * Read input.txt and return contents as a String
 */
fun Project.readInput(): String = file("input.txt").readText().trimEnd()

/**
 * Read input.txt and return lines
 */
fun Project.readLines(): List<String> = readInput().lines()

fun Project.readInputTest(): String = file("test.txt").readText().trimEnd()

fun Project.readLinesTest(): List<String> = readInputTest().lines()
