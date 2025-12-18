import java.io.File

@JvmInline
value class RawValue(val bits: UInt) {
    fun clean(targetBits: Int): Value =
        Value(bits and ((1u shl targetBits) - 1u))

    infix fun or(other: RawValue): RawValue = RawValue(bits or other.bits)
    infix fun and(other: RawValue): RawValue = RawValue(bits and other.bits)
    infix fun xor(other: RawValue): RawValue = RawValue(bits xor other.bits)
}

@JvmInline
value class Value(val bits: UInt) {
    infix fun or(other: Value): Value = Value(bits or other.bits)
    infix fun and(other: Value): Value = Value(bits and other.bits)
    infix fun xor(other: Value): Value = Value(bits xor other.bits)
}

data class Machine(
    val target: Value,
    val masks: List<Value>
)

fun getRealValue(value: UInt, targetBits: Int): UInt =
    value and ((1u shl targetBits) - 1u)

fun hammingDistance(left: Value, right: Value): Int =
    (left xor right).bits.countOneBits()

data class SearchResult(val cost: Long, val expansions: Int)

fun processMachine(machine: Machine, useHeuristic: Boolean = true): SearchResult {
    fun heuristic(v: Value): Int {
        if (!useHeuristic) return 0

        var remaining = (v xor machine.target).bits
        if (remaining == 0u) return 0

        var count = 0
        while (remaining != 0u) {
            // Optimistically: pick mask covering most remaining wrong bits
            val bestCoverage = machine.masks.maxOf { mask ->
                (mask.bits and remaining).countOneBits()
            }
            if (bestCoverage == 0) return Int.MAX_VALUE

            // Remove bestCoverage bits from remaining (optimistic assumption)
            var toRemove = bestCoverage
            var newRemaining = remaining
            var bit = 0
            while (toRemove > 0 && bit < 32) {
                if (remaining and (1u shl bit) != 0u) {
                    newRemaining = newRemaining xor (1u shl bit)
                    toRemove--
                }
                bit++
            }
            remaining = newRemaining
            count++
        }
        return count
    }

    val gScore = mutableMapOf<Value, Int>()
    gScore[Value(0U)] = 0

    val fScore = mutableMapOf<Value, Int>()
    fScore[Value(0U)] = heuristic(Value(0U))

    val openSet = java.util.PriorityQueue<Value> { a, b ->
        fScore.getOrDefault(a, Int.MAX_VALUE).compareTo(fScore.getOrDefault(b, Int.MAX_VALUE))
    }

    var expansions = 0
    openSet.add(Value(0U))
    while (openSet.isNotEmpty()) {
        val current = openSet.poll()
        expansions++
        if (current == machine.target) {
			return SearchResult(gScore[current]!!.toLong(), expansions)
		}

		machine.masks.map { it xor current }.forEach { neighbor ->
            val tentativeGScore = gScore[current]!! + 1

            if (tentativeGScore < gScore.getOrDefault(neighbor, Int.MAX_VALUE)) {
                gScore[neighbor] = tentativeGScore
                fScore[neighbor] = tentativeGScore + heuristic(neighbor)
				if (neighbor !in openSet) {
                	openSet.add(neighbor)
				}
            }
		}
    }
    return SearchResult(0L, expansions)
}

private val lineRegex = Regex("""\[([#.]+)\](.*)""")
private val maskRegex = Regex("""\(([0-9,]+)\)""")

fun parseLine(line: String): Machine {
    val match = lineRegex.find(line)!!
    val (targetStr, rest) = match.destructured
    val targetBits = targetStr.length

    val target = Value(targetStr.foldIndexed(0u) { i, acc, c ->
        if (c == '#') acc or (1u shl i) else acc
    })

    val masks = maskRegex.findAll(rest).map { m ->
        RawValue(m.groupValues[1]
            .split(",")
            .map { it.toInt() }
            .fold(0u) { acc, bit -> acc or (1u shl bit) })
            .clean(targetBits)
    }.toList()

    return Machine(target, masks)
}

fun partOne(input: List<String>) {
    val total = input
        .filter { it.isNotBlank() }
        .map { parseLine(it) }
        .parallelStream()
        .mapToLong { processMachine(it).cost }
        .sum()

    println("Answer: $total")
}

fun partTwo(input: List<String>) {
    // TODO: process lines
    println("Answer: TODO")
}

object TestCasePartOne {
    @JvmStatic
    fun main(args: Array<String>) =
        partOne(
            File("day10/src/test.txt")
                .readText()
                .trim()
                .lines()
        )
}

object SolvePartOne {
    @JvmStatic
    fun main(args: Array<String>) =
        partOne(
            File("day10/src/input.txt")
                .readText()
                .trim()
                .lines()
        )
}

object TestCasePartTwo {
    @JvmStatic
    fun main(args: Array<String>) =
        partTwo(
            File("day10/src/test.txt")
                .readText()
                .trim()
                .lines()
        )
}

object SolvePartTwo {
    @JvmStatic
    fun main(args: Array<String>) =
        partTwo(
            File("day10/src/input.txt")
                .readText()
                .trim()
                .lines()
        )
}

object CompareExpansions {
    @JvmStatic
    fun main(args: Array<String>) {
        val machines = File("day10/src/input.txt")
            .readText()
            .trim()
            .lines()
            .filter { it.isNotBlank() }
            .map { parseLine(it) }

        var totalAstar = 0L
        var totalBfs = 0L

        for (machine in machines) {
            val astarResult = processMachine(machine, useHeuristic = true)
            val bfsResult = processMachine(machine, useHeuristic = false)

            totalAstar += astarResult.expansions
            totalBfs += bfsResult.expansions

            require(astarResult.cost == bfsResult.cost) {
                "Mismatch: A*=${astarResult.cost}, BFS=${bfsResult.cost}"
            }
        }

        println("A* expansions:  $totalAstar")
        println("BFS expansions: $totalBfs")
        println("Reduction: %.2fx fewer expansions".format(totalBfs.toDouble() / totalAstar))
    }
}
