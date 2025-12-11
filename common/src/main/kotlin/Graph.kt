class Graph<T>(var adjacencyMap: MutableMap<T, List<T>> = mutableMapOf()) {
	private val parent = mutableMapOf<T, T>()
	private val rank = mutableMapOf<T, Int>()
	var componentCount: Int = 0
		private set

	private fun find(x: T): T {
		if (parent[x] != x) {
			parent[x] = find(parent[x]!!)
		}
		return parent[x]!!
	}

	private fun union(x: T, y: T): Boolean {
		val rootX = find(x)
		val rootY = find(y)
		if (rootX == rootY) return false

		val rankX = rank[rootX] ?: 0
		val rankY = rank[rootY] ?: 0
		when {
			rankX < rankY -> parent[rootX] = rootY
			rankX > rankY -> parent[rootY] = rootX
			else -> {
				parent[rootY] = rootX
				rank[rootX] = rankX + 1
			}
		}
		componentCount--
		return true
	}

	fun addNode(node: T) {
		if (!adjacencyMap.containsKey(node)) {
			adjacencyMap[node] = emptyList()
			parent[node] = node
			rank[node] = 0
			componentCount++
		}
	}

	fun inSameComponent(u: T, v: T): Boolean {
		if (!parent.containsKey(u) || !parent.containsKey(v)) return false
		return find(u) == find(v)
	}

	fun addEdge(u: T, v: T) {
		if (!parent.containsKey(u)) {
			parent[u] = u
			rank[u] = 0
			componentCount++
		}
		if (!parent.containsKey(v)) {
			parent[v] = v
			rank[v] = 0
			componentCount++
		}

		if (adjacencyMap.containsKey(u)) {
			adjacencyMap[u] = adjacencyMap[u]!! + listOf(v)
		} else {
			adjacencyMap[u] = listOf(v)
		}
		if (adjacencyMap.containsKey(v)) {
			adjacencyMap[v] = adjacencyMap[v]!! + listOf(u)
		} else {
			adjacencyMap[v] = listOf(u)
		}

		union(u, v)
	}

	fun dfs(visitor: ((node: T, componentIndex: Int) -> Unit)? = null) {
		val visited = mutableSetOf<T>()
		var componentIndex = 0

		fun dfsVisit(node: T, component: Int) {
			if (node in visited) return
			visited.add(node)
			visitor?.invoke(node, component)
			adjacencyMap[node]?.forEach { neighbor ->
				dfsVisit(neighbor, component)
			}
		}

		for (node in adjacencyMap.keys) {
			if (node !in visited) {
				dfsVisit(node, componentIndex++)
			}
		}
	}

	fun toDot(name: String = "G"): String {
		val seen = mutableSetOf<Pair<T, T>>()
		val sb = StringBuilder()
		sb.appendLine("graph $name {")
		for ((node, neighbors) in adjacencyMap) {
			for (neighbor in neighbors) {
				val edge = if (node.hashCode() <= neighbor.hashCode()) node to neighbor else neighbor to node
				if (edge !in seen) {
					seen.add(edge)
					sb.appendLine("\t\"$node\" -- \"$neighbor\";")
				}
			}
		}
		sb.appendLine("}")
		return sb.toString()
	}

	fun findPath(from: T, to: T): List<T>? {
		// Handle from == to case: only return a path if the node is actually in the graph
		if (from == to) {
			return if (from in adjacencyMap) listOf(from) else null
		}

		val visited = mutableSetOf<T>()

		fun dfsPath(node: T): List<T>? {
			if (node == to) return listOf(node)
			if (node in visited) return null
			visited.add(node)
			adjacencyMap[node]?.forEach { neighbor ->
				dfsPath(neighbor)?.let { return listOf(node) + it }
			}
			return null
		}

		return dfsPath(from)
	}
}