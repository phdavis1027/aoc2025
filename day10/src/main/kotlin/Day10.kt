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

fun processMachine(machine: Machine): Long {
    val maxBitsPerMask = machine.masks.maxOf { it.bits.countOneBits() }

    fun heuristic(v: Value): Int {
        val h = hammingDistance(v, machine.target)
        return (h + maxBitsPerMask - 1) / maxBitsPerMask  // ceil(h / maxBitsPerMask)
    }

    val gScore = mutableMapOf<Value, Int>()
    gScore[Value(0U)] = 0

    val fScore = mutableMapOf<Value, Int>()
    fScore[Value(0U)] = heuristic(Value(0U))

    val openSet = java.util.PriorityQueue<Value> { a, b ->
        fScore.getOrDefault(a, Int.MAX_VALUE).compareTo(fScore.getOrDefault(b, Int.MAX_VALUE))
    }

    openSet.add(Value(0U))
    while (openSet.isNotEmpty()) {
        val current = openSet.poll()
        if (current == machine.target) {
			return gScore[current]!!.toLong()
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
    return 0L
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
        .mapToLong { processMachine(it) }
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
