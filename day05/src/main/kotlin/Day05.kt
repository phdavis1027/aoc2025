fun RangeTree.overlapsAny(value: Long): RangeTree.Node? {
	fun check(node: RangeTree.Node?): RangeTree.Node? {
		if (node == null) return null
		if (value.overlaps(node.interval)) return node
		val left = check(node.left)
		if (left != null) return left
		return check(node.right)
	}
	return check(root)
}

fun partOne(input: List<String>) {
	val blankIndex = input.indexOf("")
	val intervalLines = input.subList(0, blankIndex)
	val valueLines = input.subList(blankIndex + 1, input.size)

	val tree = RangeTree.empty()
	for (line in intervalLines) {
		val (low, high) = line.split("-").map { it.toLong() }
		tree.insert(Interval(low, high))
	}

	val answer = valueLines.map { it.toLong() }.count { tree.overlapsAny(it) != null }
	println("Answer: $answer")
}

fun partTwo(input: List<String>) {
	val blankIndex = input.indexOf("")
	val intervalLines = input.subList(0, blankIndex)

	val tree = RangeTree.empty()
	var min = Long.MAX_VALUE
	var max = Long.MIN_VALUE
	for (line in intervalLines) {
		val (low, high) = line.split("-").map { it.toLong() }
		tree.insert(Interval(low, high))
		if (low < min) min = low
		if (high > max) max = high
	}

	var count = 0L
	for (i in min..max) {
		if (tree.overlapsAny(i) != null) count++
	}
	println("Answer: $count")
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
