import kotlin.math.max
import kotlin.math.pow

fun List<Int>.joltage(k: Int): Long {
	if (k <= 0) return 0
	if (this.size < k) return 0
	if (this.size == k) return this.toLong()
	if (k == 1) return this.max().toLong()
	var h = this[0]
	for (i in 1 until this.size) {
		if (i + k - 1 < this.size) h = max(h, this[i])
	}
	val b = this.mapIndexed { i, j ->
		if (j == h) i else -1
	}.filter { it >= 0 }
	val car = h * 10.0.pow((k - 1).toDouble()).toLong()
	val cdr = b.map{
		if (it + 1 == this.size)
			0
		else
			this.subList(
				it + 1,
				this.size
			).joltage(k - 1)
	}
	return car + cdr.max()
}

fun partOne(input: List<String>) {
	val answer: Long = input.sumOf { s ->
		s.map(Character::getNumericValue).joltage(2)
	}

	println("Answer: $answer")
}

fun partTwo(input: List<String>) {
	val answer: Long = input.sumOf { s ->
		val j = s.map(Character::getNumericValue).joltage(12)
		j
	}

	println("Answer: $answer")
}

object TestCasePartOne {
	@JvmStatic fun main(args: Array<String>) = partOne(java.io.File("src/test.txt").readText().trim().lines())
}

object JoltagePartOne {
	@JvmStatic fun main(args: Array<String>) = partOne(java.io.File("src/input.txt").readText().trim().lines())
}

object TestCasePartTwo {
	@JvmStatic fun main(args: Array<String>) = partTwo(java.io.File("src/test.txt").readText().trim().lines())
}

object JoltagePartTwo {
	@JvmStatic fun main(args: Array<String>) = partTwo(java.io.File("src/input.txt").readText().trim().lines())
}