import kotlin.collections.forEach

fun List<String>.countNeighbors(x: Int, y: Int): Int {
	var neighbors = 0
	for (row in y - 1..y + 1) {
		if (row < 0 || row >= this.size) continue
		for (col in x - 1..x + 1) {
			if (row == y && col == x) continue
			if (col < 0 || col >= this[row].length) continue
			if (this[row][col] == '@') ++neighbors
		}
	}
	return neighbors
}

fun partOne(input: List<String>) {
	var reachable = 0
	for (y in 0 until input.size) {
		val row = input[y]
		for (x in 0 until row.length) {
			if (row[x] != '@') continue
			if (input.countNeighbors(x, y) < 4) ++reachable
		}
	}

	println("Answer: $reachable")
}

fun partTwo(input: MutableList<String>) {
	var reachable = 0
	var toDelete = mutableListOf<Pair<Int, Int>>()
	do {
		toDelete.clear()
		for (y in 0 until input.size) {
			val row = input[y]
			for (x in 0 until row.length) {
				if (row[x] != '@') continue
				if (input.countNeighbors(x, y) < 4) {
					toDelete.add(Pair(x, y))
				}
			}
		}
		reachable += toDelete.size
		toDelete.forEach { point ->
			input[point.second] = input[point.second].mapIndexed { i, v ->
				if (i == point.first)
					'.'
				else
					v
			}.joinToString("")

		}
	} while(!toDelete.isEmpty())


	println("Answer: $reachable")
}

object TestCasePartOne {
	@JvmStatic fun main(args: Array<String>) =
		partOne(
		java.io.File("src/test.txt")
				.readText()
				.trim()
				.lines()
				.toMutableList()
		)

}

object SolvePartOne {
	@JvmStatic fun main(args: Array<String>) = partOne(java.io.File("src/input.txt").readText().trim().lines().toMutableList())
}

object TestCasePartTwo {
	@JvmStatic fun main(args: Array<String>) =
		partTwo(
			java.io.File("src/test.txt")
					.readText()
					.trim()
					.lines()
					.toMutableList()
		)
}

object SolvePartTwo {
	@JvmStatic fun main(args: Array<String>) = partTwo(java.io.File("src/input.txt").readText().trim().lines().toMutableList())
}
