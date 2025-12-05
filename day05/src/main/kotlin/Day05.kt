fun partOne(input: List<String>) {
	println("Answer: TODO")
}

fun partTwo(input: List<String>) {
	println("Answer: TODO")
}

object TestCasePartOne {
	@JvmStatic fun main(args: Array<String>) =
		partOne(
			java.io.File("src/test.txt")
				.readText()
				.trim()
				.lines()
		)
}

object SolvePartOne {
	@JvmStatic fun main(args: Array<String>) =
		partOne(
			java.io.File("src/input.txt")
				.readText()
				.trim()
				.lines()
		)
}

object TestCasePartTwo {
	@JvmStatic fun main(args: Array<String>) =
		partTwo(
			java.io.File("src/test.txt")
				.readText()
				.trim()
				.lines()
		)
}

object SolvePartTwo {
	@JvmStatic fun main(args: Array<String>) =
		partTwo(
			java.io.File("src/input.txt")
				.readText()
				.trim()
				.lines()
		)
}
