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
	if (k < 0 || k >= len) throw IndexOutOfBoundsException()
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

fun partOne(input: List<String>) {
	val answer = input.map {
		it.split("-").map {
			try {
				it.toLong()
			} catch (e: NumberFormatException) {
				e.printStackTrace()
				throw IllegalArgumentException("Not a number: $it")
			}
		}
	}.map { (from, to) ->
		val invalids = (from..to).filter {
			it.digits() % 2 == 0
		}.filter {
			val digits = it.digits()
			val mid = digits.floorDiv(2)
			var r = 0
			var l = mid
			while (r < mid && l < digits) {
				if (it.digitAt(r) != it.digitAt(l)) {
					return@filter false
				}
				++r
				++l
			}
			return@filter true
		}
		println("Invalid IDs in ${from}-${to}: $invalids")
		invalids
	}.flatten().fold(0, Long::plus)
	println("Answer: $answer")
}

tasks.register("testCase") {
	doLast {
		partOne(project.readDelimitedTest(","))
	}
}

tasks.register("sumInvalidIds") {
	doLast { partOne(project.readDelimited(",")) }
}