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

	val count = valueLines.map { it.toLong() }.count { tree.overlapsAny(it) != null }
	println("Answer: $count")
}

fun partTwo(input: List<String>) {
	val blankIndex = input.indexOf("")
	val intervalLines = input.subList(0, blankIndex)

	val tree = RangeTree.empty()
	for (line in intervalLines) {
		val (low, high) = line.split("-").map { it.toLong() }
		tree.insert(Interval(low, high))
	}

	var answer = 0
	// Each iteration of the root counts exactly one valid ID
	while (tree.root != null) {
		++answer
		val interval = tree.peek()
		// SAFETY: interval is only null if tree.root is null,
		// so we can safely assert it
		val n = interval!!.low
		// This loop removes all elements of `tree`
		// which contain `n`, but leaves all other integers
		// contained in exactly the same number of intervals.
		while (true) {
			val r = tree.overlapsAny(n) ?: break
			val (lo, hi) = interval.splitAt(n)
			tree.delete(r)
			if (lo != null) {
				tree.insert(lo)
			}
			if (hi != null) {
				tree.insert(hi)
			}
		}
	}

	println("Answer: $answer valid IDs")
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
