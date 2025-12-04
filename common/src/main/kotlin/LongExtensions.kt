import kotlin.math.pow

class DigitIterator(val n: Long): Iterator<Int> {
	val len = n.length()
	var k = 0

	override fun hasNext(): Boolean {
		return k < len
	}

	override fun next(): Int {
		return n.digitAt(k++)
	}
}

class DigitIterable(val n: Long): Iterable<Int> {
	override fun iterator(): Iterator<Int> = DigitIterator(n)
}

fun Long.digits(): DigitIterable {
	return DigitIterable(this)
}

fun Long.length(): Int {
	var m = 0.0
	while (Math.pow(10.0, m) <= this.toDouble()) {
		m += 1
	}
	return m.toInt()
}

fun Long.digitAt(k: Int): Int {
	val len = this.length()
	if (k < 0 || k >= len) throw IndexOutOfBoundsException("Can't get digit $k of $this with ${length()} digits")
	var digit = this
	for (i in 0 until k) {
		// Get rid of the ith digit
		digit -= digit % 10.0.pow(i.toDouble() + 1.0).toLong()
	}
	val topChop = digit % 10.0.pow(k.toDouble() + 1.0).toLong()
	// SAFETY: We know that the return result will be a single digit, so
	// we can safely cast to int
	return (topChop / 10.0.pow(k.toDouble()).toLong()).toInt()
}

fun Long.hasNSubseqs(n: Int): Boolean {
	val len = this.length()
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

fun Iterable<Int>.toLong(): Long {
	return this.fold(0L) { acc, n -> acc * 10 + n }
}
