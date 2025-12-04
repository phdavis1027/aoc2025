import kotlin.collections.flatten
import kotlin.math.pow

fun Long.digits(): Int {
	var m = 0.0
	while (Math.pow(10.0, m) <= this.toDouble()) {
		m += 1
	}
	return m.toInt()
}

fun Long.digitAt(k: Int): Int {
	val len = this.digits()
	if (k < 0 || k >= len) throw IndexOutOfBoundsException("Can't get digit $k of $this with ${digits()} digits")
	var digit = this
	for (i in 0 until k) {
		// Get rid of the ith integer
		digit -= digit % 10.0.pow(i.toDouble() + 1.0).toLong()
	}
	val topChop = digit % 10.0.pow(k.toDouble() + 1.0).toLong()
	// SAFETY: We know that the return result will be a single digit, so
	// we can safely cast to int
	return (topChop / 10.0.pow(k.toDouble()).toLong()).toInt()
}

fun Long.hasNSubseqs(n: Int): Boolean {
	val len = this.digits()
	if (n !in 0..len) return false
	if (len % n.toLong() != 0L) return false
	var pointers: MutableList<Int> =
		(0 until len step len.floorDiv(n)).toMutableList()
	while (pointers.last() < len) {
		val currents = pointers.map { this.digitAt(it) }
		if (currents.toSet().size != 1) {
			return false
		}
		pointers.replaceAll { it + 1 }
	}
	return true
}

fun partOne(input: List<String>) {
	val answer = input.map {
		it.split("-").map {
			try {
				it.toLong()
			} catch (e: NumberFormatException) {
				// This is needed because Gradle sucks and swallows
				// almost all context and also directs you to the wrong line
				// of source code if an error is thrown.
				e.printStackTrace()
				throw IllegalArgumentException("Not a number: $it")
			}
		}
	}.map { (from, to) ->
		(from..to).filter { id ->
			id.hasNSubseqs(2)
		}
	}.flatten().fold(0, Long::plus)
	println("Answer: $answer")
}

fun partTwo(input: List<String>) {
	val answer = input.map {
		it.split("-").map {
			try {
				it.toLong()
			} catch (e: NumberFormatException) {
				// This is needed because Gradle sucks and swallows
				// almost all context and also directs you to the wrong line
				// of source code if an error is thrown.
				e.printStackTrace()
				throw IllegalArgumentException("Not a number: $it")
			}
		}
	}.map { (from, to) ->
		(from..to).filter { id ->
			(2..id.digits()).any { n -> id.hasNSubseqs(n) }
		}
	}.flatten().fold(0, Long::plus)
	println("Answer: $answer")
}

object TestCasePartOne {
	@JvmStatic fun main(args: Array<String>) = partOne(java.io.File("test.txt").readText().trimEnd().split(","))
}

object SumInvalidIdsPartOne {
	@JvmStatic fun main(args: Array<String>) = partOne(java.io.File("input.txt").readText().trimEnd().split(","))
}

object TestCasePartTwo {
	@JvmStatic fun main(args: Array<String>) = partTwo(java.io.File("test.txt").readText().trimEnd().split(","))
}

object SumInvalidIdsPartTwo {
	@JvmStatic fun main(args: Array<String>) = partTwo(java.io.File("input.txt").readText().trimEnd().split(","))
}