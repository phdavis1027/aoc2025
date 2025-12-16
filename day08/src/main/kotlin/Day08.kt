fun partOne(input: List<String>, numConnections: Int) {
	val tree = input.map { line ->
		val (x, y, z) = line.split(",").map { it.toLong() }
		Point(x, y, z)
	}.toList().toThreeDTree()

	val heap = java.util.PriorityQueue<Pair<Point, Pair<Point, Int>>>(
		compareBy { it.first.distSq(it.second.first) }
	)

	for (point in tree) {
		val nearest = tree.nearestNeighbor(point, k = 1)
		if (nearest != null) {
			heap.add(point to (nearest to 1))
		}
	}

	var graph = Graph<Point>()
	val processedPairs = mutableSetOf<Set<Point>>()

	var i = 0
	while (i < numConnections) {
		val (minPoint, neighborPair) = heap.poll()
		val (minNeighbor, minK) = neighborPair

		val nextNearest = tree.nearestNeighbor(minPoint, minK + 1)
		if (nextNearest != null) {
			heap.add(minPoint to (nextNearest to minK + 1))
		}

		val pairSet = setOf(minPoint, minNeighbor)
		if (pairSet in processedPairs) {
			continue
		}
		processedPairs.add(pairSet)

		if (!graph.inSameComponent(minPoint, minNeighbor)) {
			graph.addEdge(minPoint, minNeighbor)
		}

		++i
		java.io.File("src/graph_$i.dot").writeText(graph.toDot("iteration_$i"))
	}

	var componentSizes = mutableMapOf<Int, Int>()
	graph.dfs({ _, componentIndex ->
		if (componentSizes.containsKey(componentIndex)) {
			componentSizes[componentIndex] = componentSizes[componentIndex]!! + 1
		} else {
			componentSizes[componentIndex] = 1
		}
	})

	val answer = componentSizes.values.sortedDescending().take(3).fold(1, Int::times)
	println("Answer: $answer")
}

fun partTwo(input: List<String>) {
	val points = input.map { line ->
		val (x, y, z) = line.split(",").map { it.toLong() }
		Point(x, y, z)
	}.toList()

	val tree = points.toThreeDTree()

	val heap = java.util.PriorityQueue<Pair<Point, Pair<Point, Int>>>(
		compareBy { it.first.distSq(it.second.first) }
	)

	for (point in tree) {
		val nearest = tree.nearestNeighbor(point, k = 1)
		if (nearest != null) {
			heap.add(point to (nearest to 1))
		}
	}

	val graph = Graph<Point>()

	for (point in points) {
		graph.addNode(point)
	}

	val processedPairs = mutableSetOf<Set<Point>>()

	while (graph.componentCount > 1) {
		val (minPoint, neighborPair) = heap.poll()
		val (minNeighbor, minK) = neighborPair

		val nextNearest = tree.nearestNeighbor(minPoint, minK + 1)
		if (nextNearest != null) {
			heap.add(minPoint to (nextNearest to minK + 1))
		}

		val pairSet = setOf(minPoint, minNeighbor)
		if (pairSet in processedPairs) {
			continue
		}
		processedPairs.add(pairSet)

		if (!graph.inSameComponent(minPoint, minNeighbor)) {
			graph.addEdge(minPoint, minNeighbor)
			if (graph.componentCount == 1) {
				val answer = minPoint.x * minNeighbor.x
				println("Answer: $answer")
				return
			}
		}
	}
}

object TestCasePartOne {
	@JvmStatic
	fun main(args: Array<String>) =
		partOne(
			java.io.File("src/test.txt")
				.readText()
				.trim()
				.lines(),
			numConnections = 10
		)
}

object SolvePartOne {
	@JvmStatic
	fun main(args: Array<String>) =
		partOne(
			java.io.File("src/input.txt")
				.readText()
				.trim()
				.lines(),
			numConnections = 1000
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
