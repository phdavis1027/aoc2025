import kotlin.math.abs
import java.util.PriorityQueue

/**
 * Segment tree with fractional cascading for O(lg n) edge intersection queries.
 *
 * Query: "Does any edge exist with primary coord ∈ (lo, hi) and interval intersecting (qLo, qHi)?"
 *
 * @param edges List of (primaryCoord, intervalStart, intervalEnd) - e.g., (x, y1, y2) for vertical edges
 */
class EdgeIntervalTree(edges: List<Triple<Long, Long, Long>>) {

	private class Node(
		val sortedIndices: IntArray,      // edge indices sorted by intervalStart
		val prefixMax: LongArray,         // prefix max of intervalEnd
		val leftPtr: IntArray,            // cascading pointers to left child
		val rightPtr: IntArray            // cascading pointers to right child
	)

	// Edge data: sorted by primaryCoord
	private val edgesByPrimary: List<Triple<Long, Long, Long>>
	private val nodes: Array<Node?>
	private val numLeaves: Int

	init {
		if (edges.isEmpty()) {
			edgesByPrimary = emptyList()
			nodes = emptyArray()
			numLeaves = 0
		} else {
			// Sort edges by primary coordinate
			edgesByPrimary = edges.sortedBy { it.first }

			// Compute number of leaves (next power of 2)
			numLeaves = Integer.highestOneBit(edgesByPrimary.size - 1).shl(1).coerceAtLeast(1)

			// Segment tree with 2*numLeaves nodes (1-indexed)
			nodes = arrayOfNulls(2 * numLeaves)

			// Build leaves
			for (i in 0 until numLeaves) {
				nodes[numLeaves + i] = if (i < edgesByPrimary.size) {
					val idx = i
					val intervalEnd = edgesByPrimary[idx].third
					Node(
						sortedIndices = intArrayOf(idx),
						prefixMax = longArrayOf(intervalEnd),
						leftPtr = IntArray(0),
						rightPtr = IntArray(0)
					)
				} else {
					// Empty leaf
					Node(
						sortedIndices = IntArray(0),
						prefixMax = LongArray(0),
						leftPtr = IntArray(0),
						rightPtr = IntArray(0)
					)
				}
			}

			// Build internal nodes bottom-up
			for (i in numLeaves - 1 downTo 1) {
				val left = nodes[2 * i]!!
				val right = nodes[2 * i + 1]!!
				nodes[i] = mergeNodes(left, right)
			}
		}
	}

	private fun mergeNodes(left: Node, right: Node): Node {
		val merged = mutableListOf<Int>()
		val leftPtrs = mutableListOf<Int>()
		val rightPtrs = mutableListOf<Int>()

		var li = 0
		var ri = 0

		while (li < left.sortedIndices.size || ri < right.sortedIndices.size) {
			val leftVal = if (li < left.sortedIndices.size)
				edgesByPrimary[left.sortedIndices[li]].second else Long.MAX_VALUE
			val rightVal = if (ri < right.sortedIndices.size)
				edgesByPrimary[right.sortedIndices[ri]].second else Long.MAX_VALUE

			if (leftVal <= rightVal) {
				merged.add(left.sortedIndices[li])
				li++
			} else {
				merged.add(right.sortedIndices[ri])
				ri++
			}

			leftPtrs.add(li)
			rightPtrs.add(ri)
		}

		val sortedIndices = merged.toIntArray()
		val prefixMax = LongArray(sortedIndices.size)
		var maxSoFar = Long.MIN_VALUE

		for (i in sortedIndices.indices) {
			maxSoFar = maxOf(maxSoFar, edgesByPrimary[sortedIndices[i]].third)
			prefixMax[i] = maxSoFar
		}

		return Node(
			sortedIndices = sortedIndices,
			prefixMax = prefixMax,
			leftPtr = leftPtrs.toIntArray(),
			rightPtr = rightPtrs.toIntArray()
		)
	}

	fun hasIntersection(lo: Long, hi: Long, qLo: Long, qHi: Long): Boolean {
		if (edgesByPrimary.isEmpty()) return false

		val root = nodes[1] ?: return false
		if (root.sortedIndices.isEmpty()) return false

		val rootPos = binarySearchLessThan(root.sortedIndices, qHi)
		if (rootPos < 0) return false

		val leftIdx = lowerBoundPrimary(lo)
		val rightIdx = upperBoundPrimary(hi)

		if (leftIdx > rightIdx) return false

		return queryNode(1, 0, numLeaves - 1, leftIdx, rightIdx, rootPos, qLo)
	}

	private fun binarySearchLessThan(sortedIndices: IntArray, threshold: Long): Int {
		var lo = 0
		var hi = sortedIndices.size
		while (lo < hi) {
			val mid = (lo + hi) / 2
			if (edgesByPrimary[sortedIndices[mid]].second < threshold) {
				lo = mid + 1
			} else {
				hi = mid
			}
		}
		return lo - 1
	}

	private fun lowerBoundPrimary(lo: Long): Int {
		var left = 0
		var right = edgesByPrimary.size
		while (left < right) {
			val mid = (left + right) / 2
			if (edgesByPrimary[mid].first <= lo) {
				left = mid + 1
			} else {
				right = mid
			}
		}
		return left
	}

	private fun upperBoundPrimary(hi: Long): Int {
		var left = 0
		var right = edgesByPrimary.size
		while (left < right) {
			val mid = (left + right) / 2
			if (edgesByPrimary[mid].first < hi) {
				left = mid + 1
			} else {
				right = mid
			}
		}
		return left - 1
	}

	private fun queryNode(
		nodeIdx: Int,
		nodeLeft: Int,
		nodeRight: Int,
		queryLeft: Int,
		queryRight: Int,
		pos: Int,
		qLo: Long
	): Boolean {
		if (queryLeft > queryRight || queryRight < nodeLeft || queryLeft > nodeRight) {
			return false
		}

		val node = nodes[nodeIdx] ?: return false

		val localPos = when {
			nodeIdx == 1 -> pos
			pos < 0 -> -1
			else -> pos - 1
		}

		if (localPos < 0 || node.sortedIndices.isEmpty()) return false

		if (queryLeft <= nodeLeft && nodeRight <= queryRight) {
			val actualPos = minOf(localPos, node.sortedIndices.size - 1)
			return node.prefixMax[actualPos] > qLo
		}

		if (nodeIdx >= numLeaves) return false

		val mid = (nodeLeft + nodeRight) / 2
		val leftChild = 2 * nodeIdx
		val rightChild = 2 * nodeIdx + 1

		val leftPos = if (localPos >= 0 && localPos < node.leftPtr.size)
			node.leftPtr[localPos] else 0
		val rightPos = if (localPos >= 0 && localPos < node.rightPtr.size)
			node.rightPtr[localPos] else 0

		return queryNode(leftChild, nodeLeft, mid, queryLeft, queryRight, leftPos, qLo) ||
			   queryNode(rightChild, mid + 1, nodeRight, queryLeft, queryRight, rightPos, qLo)
	}
}

@JvmInline
value class ChebyshevPoint(private val packed: Pair<Long, Long>) {
	val u: Long get() = packed.first
	val v: Long get() = packed.second

	companion object {
		fun fromCartesian(x: Long, y: Long) = ChebyshevPoint((x + y) to (x - y))
	}

	fun toCartesian(): Pair<Long, Long> = ((u + v) / 2) to ((u - v) / 2)
}

fun chebyshevDistance(p1: Pair<Long, Long>, p2: Pair<Long, Long>): Long =
	maxOf(abs(p1.first - p2.first), abs(p1.second - p2.second))

fun partOne(input: List<String>) {
	val points = input.map {
		val (x, y) = it.split(",")
		ChebyshevPoint.fromCartesian(x.toLong(), y.toLong())
	}

	val minUPoint = points.minBy { it.u }
	val maxUPoint = points.maxBy { it.u }
	val uDistance = maxUPoint.u - minUPoint.u

	val minVPoint = points.minBy { it.v }
	val maxVPoint = points.maxBy { it.v }
	val vDistance = maxVPoint.v - minVPoint.v

	val (p1, p2) = if (uDistance > vDistance) {
		minUPoint.toCartesian() to maxUPoint.toCartesian()
	} else {
		minVPoint.toCartesian() to maxVPoint.toCartesian()
	}

	val area = (abs(p1.first - p2.first) + 1) * (abs(p1.second - p2.second) + 1)

	println("Answer: $area")
}

private data class Candidate(
	val distance: Long,    // Manhattan distance
	val i: Int,            // first point index
	val j: Int,            // second point index
	val source: Char,      // 'u' or 'v' dimension
	val left: Int,         // left index in sorted array
	val right: Int         // right index in sorted array
)

fun partTwo(input: List<String>) {
	val points = input.map {
		val (x, y) = it.split(",")
		x.toLong() to y.toLong()
	}

	val n = points.size

	val verticalEdges = mutableListOf<Triple<Long, Long, Long>>()   // (x, y1, y2)
	val horizontalEdges = mutableListOf<Triple<Long, Long, Long>>() // (y, x1, x2)

	for (i in points.indices) {
		val (x1, y1) = points[i]
		val (x2, y2) = points[(i + 1) % n]
		if (x1 == x2) {
			verticalEdges.add(Triple(x1, minOf(y1, y2), maxOf(y1, y2)))
		} else {
			horizontalEdges.add(Triple(y1, minOf(x1, x2), maxOf(x1, x2)))
		}
	}

	val vertTree = EdgeIntervalTree(verticalEdges)
	val horizTree = EdgeIntervalTree(horizontalEdges)

	val chebyshevPoints = points.map { (x, y) -> ChebyshevPoint.fromCartesian(x, y) }

	val byU = (0 until n).sortedBy { chebyshevPoints[it].u }
	val byV = (0 until n).sortedBy { chebyshevPoints[it].v }

	fun manhattan(i: Int, j: Int): Long {
		val (x1, y1) = points[i]
		val (x2, y2) = points[j]
		return abs(x1 - x2) + abs(y1 - y2)
	}

	fun isValidRectangle(i: Int, j: Int): Boolean {
		val (x1, y1) = points[i]
		val (x2, y2) = points[j]

		val minX = minOf(x1, x2)
		val maxX = maxOf(x1, x2)
		val minY = minOf(y1, y2)
		val maxY = maxOf(y1, y2)

		// Check: no vertical edge in interior (x ∈ (minX, maxX), y-range intersects (minY, maxY))
		if (vertTree.hasIntersection(minX, maxX, minY, maxY)) return false

		// Check: no horizontal edge in interior (y ∈ (minY, maxY), x-range intersects (minX, maxX))
		if (horizTree.hasIntersection(minY, maxY, minX, maxX)) return false

		return true
	}

	// Max-heap by distance (descending)
	val heap = PriorityQueue<Candidate>(compareByDescending { it.distance })
	val seen = mutableSetOf<Pair<Int, Int>>()

	// Add candidate to heap if valid
	fun addCandidate(left: Int, right: Int, source: Char, sortedArr: List<Int>) {
		if (left < right) {
			val i = sortedArr[left]
			val j = sortedArr[right]
			val pair = if (i < j) i to j else j to i
			if (pair !in seen) {
				val dist = manhattan(i, j)
				heap.add(Candidate(dist, pair.first, pair.second, source, left, right))
			}
		}
	}

	addCandidate(0, n - 1, 'u', byU)
	addCandidate(0, n - 1, 'v', byV)

	// Process pairs in decreasing Manhattan distance order
	while (heap.isNotEmpty()) {
		val (_, i, j, source, left, right) = heap.poll()
		val pair = i to j

		if (pair in seen) continue
		seen.add(pair)

		if (isValidRectangle(i, j)) {
			val (x1, y1) = points[i]
			val (x2, y2) = points[j]
			val area = (abs(x1 - x2) + 1) * (abs(y1 - y2) + 1)
			println("Answer: $area")
			return
		}

		// Expand frontier in the source dimension
		val sortedArr = if (source == 'u') byU else byV
		addCandidate(left + 1, right, source, sortedArr)
		addCandidate(left, right - 1, source, sortedArr)
	}

	// No valid rectangle found
	println("Answer: 0")
}

object TestCasePartOne {
	@JvmStatic
	fun main(args: Array<String>) =
		partOne(
			java.io.File("src/test.txt")
				.readText()
				.trim()
				.lines()
		)
}

object SolvePartOne {
	@JvmStatic
	fun main(args: Array<String>) =
		partOne(
			java.io.File("src/input.txt")
				.readText()
				.trim()
				.lines()
		)
}

object TestCasePartTwo {
	@JvmStatic
	fun main(args: Array<String>) =
		partTwo(
			java.io.File("src/test.txt")
				.readText()
				.trim()
				.lines()
		)
}

object SolvePartTwo {
	@JvmStatic
	fun main(args: Array<String>) =
		partTwo(
			java.io.File("src/input.txt")
				.readText()
				.trim()
				.lines()
		)
}
