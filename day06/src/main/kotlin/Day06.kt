fun partOne(input: List<String>) {
	val numProbs = input.first().split(" ").count { it.isNotBlank() }
	val ops = input.last().split(" ").filter { it.isNotBlank() }
	val answer = input.dropLast(1).fold(MutableList(numProbs) { mutableListOf<Long>() }, { acc, v ->
		v.split(" ").filter { it.isNotBlank() }.map { it.toLong() }.forEachIndexed { i, n ->
			acc[i].add(n)
		}
		acc
	}).mapIndexed { col, n ->
		when (ops[col]) {
			"*" -> n.fold(1, Long::times)
			"+" -> n.fold(0, Long::plus)
			else -> throw IllegalArgumentException("Unknown op $ops")
		}
	}.fold(0, Long::plus)
	println("Answer: $answer")
}

fun partTwo(input: List<String>) {
	// Find operators and their column positions in the last row
	val lastRow = input.last()
	val ops = lastRow.mapIndexedNotNull { col, c ->
		if (c == '*' || c == '+') col to c.toString() else null
	}

	val dataRows = input.dropLast(1)
	val maxCols = dataRows.maxOf { it.length }
	val colValues = dataRows.fold(MutableList<Long?>(maxCols) { null }) { acc, row ->
		row.forEachIndexed { col, char ->
			if (char.isDigit()) {
				val digit = (char - '0').toLong()
				acc[col] = (acc[col] ?: 0L) * 10 + digit
			}
		}
		acc
	}

	val answer = ops.mapIndexed { i, (opCol, op) ->
		val endCol = if (i + 1 < ops.size) ops[i + 1].first else maxCols
		val nums = (opCol until endCol).mapNotNull { colValues[it] }
		when (op) {
			"*" -> nums.fold(1L, Long::times)
			"+" -> nums.fold(0L, Long::plus)
			else -> throw IllegalArgumentException("Unknown op $op")
		}
	}.fold(0L, Long::plus)

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
