import kotlin.math.max
import kotlin.math.pow

fun List<Int>.joltage(k: Int): Int {
	if (k <= 0) return 0
	if (this.size < k) return 0
	if (this.size == k) return this.toInt()
	var h = this[0]
	for (i in 1 until this.size) {
		if (i + k - 1 < this.size) h = max(h, this[i])
	}
	val b = this.mapIndexed { i, j ->
		if (j == h) i else -1
	}.filter { it >= 0 }
	val car = h * 10.0.pow((k - 1).toDouble()).toInt()
	val cdr = b.map{
		this.subList(
			it + 1,
			this.size
		).joltage(k - 1)
	}.max()
	return car + cdr
}

fun partOne(input: List<String>) {
	val answer: Int = input.sumOf { s ->
		s.map(Character::getNumericValue).joltage(2)
	}

	println("Answer: $answer")
}

object TestCasePartOne {
	@JvmStatic fun main(args: Array<String>) = partOne(java.io.File("src/test.txt").readText().trim().lines())
}

object JoltagePartOne {
	@JvmStatic fun main(args: Array<String>) = partOne(java.io.File("src/input.txt").readText().trim().lines())
}