data class Point(val x: Long, val y: Long, val z: Long)

fun Point.distSq(other: Point): Long {
	val dx = this.x - other.x
	val dy = this.y - other.y
	val dz = this.z - other.z
	return dx * dx + dy * dy + dz * dz
}

fun Point.superKeyCompare(other: Point, p: Int): Int {
	for (i in 0 until 3) {
		val dim = (p + i) % 3
		val cmp = when (dim) {
			0 -> this.x.compareTo(other.x)
			1 -> this.y.compareTo(other.y)
			2 -> this.z.compareTo(other.z)
			else -> throw AssertionError("Unreachable")
		}
		if (cmp != 0) return cmp
	}
	return 0
}

fun List<Point>.kInitialMergeSorts(): Array<IntArray> {
	val n = this.size
	val points = this

	return Array(3) { p ->
		val indices = IntArray(n) { it }

		mergeSort(indices, IntArray(n), 0, n, points, p)

		indices
	}
}

fun deduplicateIndexArrays(
	points: List<Point>,
	indexArrays: Array<IntArray>
): Array<IntArray> {
	if (points.isEmpty()) return indexArrays

	val sorted0 = indexArrays[0]
	val n = sorted0.size

	val keep = BooleanArray(points.size)

	keep[sorted0[0]] = true

	for (i in 1 until n) {
		val prevIdx = sorted0[i - 1]
		val currIdx = sorted0[i]
		val prev = points[prevIdx]
		val curr = points[currIdx]
		if (prev.x != curr.x || prev.y != curr.y || prev.z != curr.z) {
			keep[currIdx] = true
		}
	}

	var uniqueCount = 0
	for (k in keep) if (k) uniqueCount++

	return Array(3) { p ->
		val result = IntArray(uniqueCount)
		var j = 0
		for (idx in indexArrays[p]) {
			if (keep[idx]) {
				result[j++] = idx
			}
		}
		result
	}
}

fun buildKdTree(
	points: List<Point>,
	indexArrays: Array<IntArray>,
	depth: Int = 0
): ThreeDTree.Node? {
	val n = indexArrays[0].size
	if (n == 0) return null

	val p = depth % 3

	when (n) {
		1 -> {
			return ThreeDTree.Node(points[indexArrays[p][0]], depth = depth)
		}

		2 -> {
			val node = ThreeDTree.Node(points[indexArrays[p][1]], depth = depth)
			node.left = ThreeDTree.Node(points[indexArrays[p][0]], depth = depth + 1)
			return node
		}

		3 -> {
			val node = ThreeDTree.Node(points[indexArrays[p][1]], depth = depth)
			node.left = ThreeDTree.Node(points[indexArrays[p][0]], depth = depth + 1)
			node.right = ThreeDTree.Node(points[indexArrays[p][2]], depth = depth + 1)
			return node
		}
	}

	val medianIdx = n / 2
	val medianPointIdx = indexArrays[p][medianIdx]
	val medianPoint = points[medianPointIdx]

	val node = ThreeDTree.Node(medianPoint, depth = depth)

	val leftSize = medianIdx
	val rightSize = n - medianIdx - 1

	val leftArrays = Array(3) { q ->
		if (q == p) {
			indexArrays[p].copyOfRange(0, medianIdx)
		} else {
			val result = IntArray(leftSize)
			var j = 0
			for (idx in indexArrays[q]) {
				if (idx == medianPointIdx) continue
				if (points[idx].superKeyCompare(medianPoint, p) < 0) {
					result[j++] = idx
				}
			}
			result
		}
	}

	val rightArrays = Array(3) { q ->
		if (q == p) {
			indexArrays[p].copyOfRange(medianIdx + 1, n)
		} else {
			val result = IntArray(rightSize)
			var j = 0
			for (idx in indexArrays[q]) {
				if (idx == medianPointIdx) continue
				if (points[idx].superKeyCompare(medianPoint, p) > 0) {
					result[j++] = idx
				}
			}
			result
		}
	}

	node.left = buildKdTree(points, leftArrays, depth + 1)
	node.right = buildKdTree(points, rightArrays, depth + 1)

	return node
}

private fun mergeSort(
	indices: IntArray,
	temp: IntArray,
	start: Int,
	end: Int,
	points: List<Point>,
	p: Int
) {
	if (end - start <= 1) return

	val mid = start + (end - start) / 2
	mergeSort(indices, temp, start, mid, points, p)
	mergeSort(indices, temp, mid, end, points, p)
	merge(indices, temp, start, mid, end, points, p)
}

private fun merge(
	indices: IntArray,
	temp: IntArray,
	start: Int,
	mid: Int,
	end: Int,
	points: List<Point>,
	p: Int
) {
	// Copy to temp
	for (i in start until end) {
		temp[i] = indices[i]
	}

	var left = start
	var right = mid
	var dest = start

	while (left < mid && right < end) {
		val cmp = points[temp[left]].superKeyCompare(points[temp[right]], p)
		if (cmp <= 0) {
			indices[dest++] = temp[left++]
		} else {
			indices[dest++] = temp[right++]
		}
	}

	while (left < mid) {
		indices[dest++] = temp[left++]
	}
}

class ThreeDTree : Iterable<Point> {
	var root: Node? = null

	class Node(var data: Point, var left: Node? = null, var right: Node? = null, var depth: Int = 0) :
		Comparable<Node> {
		override fun compareTo(other: Node): Int {
			assert(this.depth == other.depth)
			return when (depth % 3) {
				0 -> this.data.x.compareTo(other.data.x)
				1 -> this.data.y.compareTo(other.data.y)
				2 -> this.data.z.compareTo(other.data.z)
				else -> throw AssertionError("Unreachable: 0 <= x % 3 < 3")
			}
		}
	}

	fun insert(point: Point): ThreeDTree {
		this.root = this.insertNode(point, this.root, 0)
		return this
	}

	private fun insertNode(point: Point, node: Node?, depth: Int): Node {
		if (node == null) {
			return Node(point, depth = depth)
		}
		val toInsert = Node(point, depth = depth)
		if (toInsert < node) {
			node.left = this.insertNode(point, node.left, depth + 1)
		} else {
			node.right = this.insertNode(point, node.right, depth + 1)
		}
		return node
	}

	fun nearestNeighbor(query: Point, k: Int = 0): Point? {
		if (root == null || k < 0) return null

		val heap = java.util.PriorityQueue<Pair<Point, Long>>(compareByDescending { it.second })

		fun coord(p: Point, dim: Int) = when (dim) {
			0 -> p.x
			1 -> p.y
			else -> p.z
		}

		fun search(node: Node?) {
			if (node == null) return

			val d = query.distSq(node.data)
			heap.add(node.data to d)
			if (heap.size > k + 1) heap.poll()

			val dim = node.depth % 3
			val diff = coord(query, dim) - coord(node.data, dim)

			val (near, far) = if (diff < 0) node.left to node.right else node.right to node.left
			search(near)

			val planeDistSq = diff * diff
			val threshold = if (heap.size < k + 1) Long.MAX_VALUE else heap.peek().second
			if (planeDistSq < threshold) {
				search(far)
			}
		}

		search(root)

		return if (heap.size > k) heap.peek().first else null
	}

	override fun iterator(): Iterator<Point> = object : Iterator<Point> {
		private val stack = ArrayDeque<Node>()

		init {
			pushLeftPath(root)
		}

		private fun pushLeftPath(node: Node?) {
			var current = node
			while (current != null) {
				stack.addLast(current)
				current = current.left
			}
		}

		override fun hasNext(): Boolean = stack.isNotEmpty()

		override fun next(): Point {
			if (!hasNext()) throw NoSuchElementException()
			val node = stack.removeLast()
			pushLeftPath(node.right)
			return node.data
		}
	}
}

fun List<Point>.toThreeDTree(): ThreeDTree {
	val tree = ThreeDTree()
	if (this.isEmpty()) return tree

	// Step 1: k initial merge sorts
	val indexArrays = this.kInitialMergeSorts()

	// Step 2: Deduplicate
	val dedupedArrays = deduplicateIndexArrays(this, indexArrays)

	// Step 3: Recursively build the tree
	tree.root = buildKdTree(this, dedupedArrays, depth = 0)

	return tree
}