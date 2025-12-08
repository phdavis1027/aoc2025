fun partOne(input: List<String>) {
	val blankIndex = input.indexOf("")
	val intervalLines = input.subList(0, blankIndex)
	val valueLines = input.subList(blankIndex + 1, input.size)

	val tree = RangeTree.empty()
	for (line in intervalLines) {
		val (low, high) = line.split("-").map { it.toLong() }
		tree.insert(Interval(low, high))
	}

	val answer = valueLines.map { it.toLong() }.count { tree.intervalSearch(it) != null }
	println("Answer: $answer")
}

fun partTwo(input: List<String>) {
	val blankIndex = input.indexOf("")
	val intervalLines = input.subList(0, blankIndex)

	val tree = RangeTree.empty()
	for (line in intervalLines) {
		val (low, high) = line.split("-").map { it.toLong() }
		tree.insert(Interval(low, high))
	}

	val answer = tree.sumLengths()
	println("Answer: $answer")
}

object TestCasePartOne {
	@JvmStatic
	fun main(args: Array<String>) =
		partOne(
			java.io.File("src/test.txt")
				.readText()
				.trim()
				.lines()
		)
}

object SolvePartOne {
	@JvmStatic
	fun main(args: Array<String>) =
		partOne(
			java.io.File("src/input.txt")
				.readText()
				.trim()
				.lines()
		)
}

object TestCasePartTwo {
	@JvmStatic
	fun main(args: Array<String>) =
		partTwo(
			java.io.File("src/test.txt")
				.readText()
				.trim()
				.lines()
		)
}

object SolvePartTwo {
	@JvmStatic
	fun main(args: Array<String>) =
		partTwo(
			java.io.File("src/input.txt")
				.readText()
				.trim()
				.lines()
		)
}
