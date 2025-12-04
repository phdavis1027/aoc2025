import kotlin.collections.flatten

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
			(2..id.length()).any { n -> id.hasNSubseqs(n) }
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