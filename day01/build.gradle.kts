import kotlin.math.absoluteValue

enum class Direction(val valence: Int) {
	LEFT(-1),
	RIGHT(1);
}



data class Movement(val dir: Direction, val extent: Int) {
	companion object {
		fun fromString(s: String): Movement? {
			val ext = s.slice(1..s.length-1).toInt()
			val dir = when(s.get(0)) {
				'L' -> Direction.LEFT
				'R' -> Direction.RIGHT
				else -> return null
			}
			return Movement(dir, ext)
		}
	}

	fun applyTo(v: Int): Int {
		return (applyToNoMod(v) % 100 + 100) % 100
	}

	fun applyToNoMod(v: Int): Int {
		return v + dir.valence * extent
	}
}

var testLines: List<String> = emptyList()
var testMovements: List<Movement?> = emptyList()
var testFinalZeros = 0
var testTotalZeros = 0
var testState_ = 50

var lines: List<String> = emptyList()
var movements: List<Movement?> = emptyList()
var finalZeros = 0
var totalZeros = 0
var state_ = 50

tasks.register("readLines") {
    doLast {
        lines = project.readLines()
		testLines = project.readLinesTest()
    }
}

tasks.register("parseMovements") {
	dependsOn("readLines")

	doLast {
		movements = lines.map { Movement.fromString(it) }
		testMovements = testLines.map { Movement.fromString(it) }
	}
}

tasks.register("testCase") {
	dependsOn("parseMovements")

	doLast {
		for (mov in testMovements.filterNotNull()) {
			println("Movement: $mov")
			println("State: $testState_")
			val distance = mov.dir.valence * mov.extent
			assert(distance.absoluteValue.floorDiv(100) + ((distance % 100 + 100) % 100) == distance)
			println("Adding minimum distance ${distance.absoluteValue.floorDiv(100)}")
			testTotalZeros += distance.absoluteValue.floorDiv(100)
			val rest = testState_ + (distance % 100)
			println("Rest $rest")
			if (rest !in 1 until 100 && testState_ != 0) {
				println("Incrementing total zeros based on rest")
				testTotalZeros++
			}
			testState_ = ((testState_ + distance) % 100 + 100) % 100
			if (testState_ == 0) {
				testFinalZeros++
			}
			println("---\n")
		}

		println("Test final zeros: $testFinalZeros")
		println("Test all zeros: $testTotalZeros")
	}
}

tasks.register("countZeros") {
	dependsOn("parseMovements")

	doLast {
		for (mov in movements.filterNotNull()) {
			println("Movement: $mov")
			println("State: $state_")
			val distance = mov.dir.valence * mov.extent
			assert(distance.absoluteValue.floorDiv(100) + ((distance % 100 + 100) % 100) == distance)
			println("Adding minimum distance ${distance.absoluteValue.floorDiv(100)}")
			totalZeros += distance.absoluteValue.floorDiv(100)
			val rest = state_ + (distance % 100)
			println("Rest $rest")
			if (rest !in 1 until 100 && state_ != 0) {
				println("Incrementing total zeros based on rest")
				totalZeros++
			}
			state_ = ((state_ + distance) % 100 + 100) % 100
			if (state_ == 0) {
				finalZeros++
			}
			println("---\n")
		}

		println("Final zeros: $finalZeros")
		println("All zeros: $totalZeros")
	}
}

