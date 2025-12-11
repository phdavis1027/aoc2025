import io.kotest.core.spec.style.StringSpec
import io.kotest.property.Arb
import io.kotest.property.arbitrary.bind
import io.kotest.property.arbitrary.list
import io.kotest.property.arbitrary.long
import io.kotest.property.arbitrary.pair
import io.kotest.property.checkAll

class GraphTest : StringSpec({

    val pointArb = Arb.bind(
        Arb.long(-1000L..1000L),
        Arb.long(-1000L..1000L),
        Arb.long(-1000L..1000L)
    ) { x, y, z -> Point(x, y, z) }

    "findPath returns null for node not in graph, even when from equals to" {
        checkAll(pointArb) { node: Point ->
            val graph = Graph<Point>()
            val path = graph.findPath(node, node)
            assert(path == null) {
                "findPath($node, $node) should return null when node is not in graph, got $path"
            }
        }
    }

    "findPath returns path to self when node IS in graph" {
        checkAll(pointArb, pointArb) { a: Point, b: Point ->
            if (a == b) return@checkAll

            val graph = Graph<Point>()
            graph.addEdge(a, b)

            val pathAA = graph.findPath(a, a)
            val pathBB = graph.findPath(b, b)

            assert(pathAA != null) { "findPath($a, $a) should return path when $a is in graph" }
            assert(pathBB != null) { "findPath($b, $b) should return path when $b is in graph" }
        }
    }

    "findPath returns valid path between directly connected nodes" {
        checkAll(pointArb, pointArb) { a: Point, b: Point ->
            if (a == b) return@checkAll

            val graph = Graph<Point>()
            graph.addEdge(a, b)

            val pathAB = graph.findPath(a, b)
            val pathBA = graph.findPath(b, a)

            assert(pathAB != null) { "findPath($a, $b) should find a path after addEdge($a, $b)" }
            assert(pathBA != null) { "findPath($b, $a) should find a path after addEdge($a, $b)" }

            // Path should start at 'from' and end at 'to'
            assert(pathAB!!.first() == a) { "Path from $a to $b should start at $a, got ${pathAB.first()}" }
            assert(pathAB.last() == b) { "Path from $a to $b should end at $b, got ${pathAB.last()}" }
        }
    }

    "findPath returns null for disconnected nodes" {
        checkAll(pointArb, pointArb, pointArb, pointArb) { a: Point, b: Point, c: Point, d: Point ->
            val nodes = setOf(a, b, c, d)
            if (nodes.size < 4) return@checkAll

            val graph = Graph<Point>()
            graph.addEdge(a, b)  // Component 1
            graph.addEdge(c, d)  // Component 2

            // Within same component: path should exist
            assert(graph.findPath(a, b) != null)
            assert(graph.findPath(c, d) != null)

            // Across components: no path should exist
            assert(graph.findPath(a, c) == null) { "findPath($a, $c) should be null for disconnected nodes" }
            assert(graph.findPath(a, d) == null) { "findPath($a, $d) should be null for disconnected nodes" }
            assert(graph.findPath(b, c) == null) { "findPath($b, $c) should be null for disconnected nodes" }
            assert(graph.findPath(b, d) == null) { "findPath($b, $d) should be null for disconnected nodes" }
        }
    }

    "findPath returns null when from is not in graph but to is" {
        checkAll(pointArb, pointArb, pointArb) { a: Point, b: Point, outside: Point ->
            if (a == b || a == outside || b == outside) return@checkAll

            val graph = Graph<Point>()
            graph.addEdge(a, b)

            val path = graph.findPath(outside, a)
            assert(path == null) {
                "findPath($outside, $a) should be null when $outside is not in graph"
            }
        }
    }

    "findPath returns null when to is not in graph but from is" {
        checkAll(pointArb, pointArb, pointArb) { a: Point, b: Point, outside: Point ->
            if (a == b || a == outside || b == outside) return@checkAll

            val graph = Graph<Point>()
            graph.addEdge(a, b)

            val path = graph.findPath(a, outside)
            assert(path == null) {
                "findPath($a, $outside) should be null when $outside is not in graph"
            }
        }
    }

    "findPath finds path through intermediate nodes" {
        checkAll(pointArb, pointArb, pointArb) { a: Point, b: Point, c: Point ->
            val nodes = setOf(a, b, c)
            if (nodes.size < 3) return@checkAll

            val graph = Graph<Point>()
            graph.addEdge(a, b)
            graph.addEdge(b, c)

            // a and c are connected through b
            val pathAC = graph.findPath(a, c)
            val pathCA = graph.findPath(c, a)

            assert(pathAC != null) { "findPath($a, $c) should find path through $b" }
            assert(pathCA != null) { "findPath($c, $a) should find path through $b" }

            // Verify path structure
            assert(pathAC!!.first() == a) { "Path should start at $a" }
            assert(pathAC.last() == c) { "Path should end at $c" }
        }
    }

    "findPath returned path has valid edges" {
        val pointPairArb = Arb.pair(pointArb, pointArb)
        val edgesArb = Arb.list(pointPairArb, 1..30)

        checkAll(edgesArb) { edges: List<Pair<Point, Point>> ->
            val graph = Graph<Point>()
            for ((u, v) in edges) {
                if (u != v) graph.addEdge(u, v)
            }

            val nodes = graph.adjacencyMap.keys.toList()
            if (nodes.size < 2) return@checkAll

            for (from in nodes.take(5)) {
                for (to in nodes.take(5)) {
                    val path = graph.findPath(from, to)

                    if (path != null) {
                        assert(path.first() == from) {
                            "Path should start at $from, got ${path.first()}"
                        }
                        assert(path.last() == to) {
                            "Path should end at $to, got ${path.last()}"
                        }

                        for (i in 0 until path.size - 1) {
                            val u = path[i]
                            val v = path[i + 1]
                            val neighbors = graph.adjacencyMap[u] ?: emptyList()
                            assert(v in neighbors) {
                                "Invalid path: no edge from $u to $v. Path: $path"
                            }
                        }
                    }
                }
            }
        }
    }

    "findPath agrees with DFS component membership" {
        val pointPairArb = Arb.pair(pointArb, pointArb)
        val edgesArb = Arb.list(pointPairArb, 1..20)

        checkAll(edgesArb) { edges: List<Pair<Point, Point>> ->
            val graph = Graph<Point>()
            for ((u, v) in edges) {
                if (u != v) graph.addEdge(u, v)
            }

            val nodeToComponent = mutableMapOf<Point, Int>()
            graph.dfs { node, componentIndex ->
                nodeToComponent[node] = componentIndex
            }

            val nodes = graph.adjacencyMap.keys.toList()

            for (a in nodes) {
                for (b in nodes) {
                    val path = graph.findPath(a, b)
                    val sameComponent = nodeToComponent[a] == nodeToComponent[b]

                    assert((path != null) == sameComponent) {
                        "findPath($a, $b) returned ${if (path != null) "path" else "null"}, " +
                        "but nodes are ${if (sameComponent) "in same" else "in different"} component(s). " +
                        "Components: $a -> ${nodeToComponent[a]}, $b -> ${nodeToComponent[b]}"
                    }
                }
            }
        }
    }
})
