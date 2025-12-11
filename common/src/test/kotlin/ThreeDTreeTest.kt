import io.kotest.core.spec.style.StringSpec
import io.kotest.property.Arb
import io.kotest.property.arbitrary.bind
import io.kotest.property.arbitrary.list
import io.kotest.property.arbitrary.long
import io.kotest.property.checkAll

class ThreeDTreeTest : StringSpec({

    "kInitialMergeSorts produces correctly sorted index arrays for all three super keys" {
        val pointArb = Arb.bind(
            Arb.long(-1000L..1000L),
            Arb.long(-1000L..1000L),
            Arb.long(-1000L..1000L)
        ) { x, y, z -> Point(x, y, z) }

        val pointsArb = Arb.list(pointArb, 1..100)

        checkAll(pointsArb) { points: List<Point> ->

            val indexArrays = points.kInitialMergeSorts()

            // Verify each of the 3 index arrays is sorted by its super key
            for (p in 0 until 3) {
                val indices = indexArrays[p]

                // Check that indices are a permutation of 0 until n
                assert(indices.sorted() == (0 until points.size).toList()) {
                    "Index array $p is not a permutation of 0..${points.size - 1}"
                }

                // Check that consecutive elements are in non-decreasing order by super key p
                for (i in 0 until indices.size - 1) {
                    val cmp = points[indices[i]].superKeyCompare(points[indices[i + 1]], p)
                    assert(cmp <= 0) {
                        "Index array $p not sorted at position $i: " +
                        "points[${indices[i]}]=${points[indices[i]]} should be <= " +
                        "points[${indices[i + 1]}]=${points[indices[i + 1]]} by super key $p"
                    }
                }
            }
        }
    }

    "deduplicateIndexArrays removes duplicates and preserves sort order" {
        val pointArb = Arb.bind(
            Arb.long(-100L..100L),  // Smaller range to encourage duplicates
            Arb.long(-100L..100L),
            Arb.long(-100L..100L)
        ) { x, y, z -> Point(x, y, z) }

        val pointsArb = Arb.list(pointArb, 1..100)

        checkAll(pointsArb) { points: List<Point> ->
            val indexArrays = points.kInitialMergeSorts()
            val dedupedArrays = deduplicateIndexArrays(points, indexArrays)

            // All three deduped arrays should have the same length
            val uniqueCount = dedupedArrays[0].size
            assert(dedupedArrays[1].size == uniqueCount)
            assert(dedupedArrays[2].size == uniqueCount)

            // Count actual unique points
            val uniquePoints = points.toSet()
            assert(uniqueCount == uniquePoints.size) {
                "Expected ${uniquePoints.size} unique points but got $uniqueCount"
            }

            // Each deduped array should still be sorted by its super key
            for (p in 0 until 3) {
                val indices = dedupedArrays[p]
                for (i in 0 until indices.size - 1) {
                    val cmp = points[indices[i]].superKeyCompare(points[indices[i + 1]], p)
                    assert(cmp <= 0) {
                        "Deduped array $p not sorted at position $i"
                    }
                }
            }

            // The points referenced by the deduped indices should all be unique
            for (p in 0 until 3) {
                val referencedPoints = dedupedArrays[p].map { points[it] }
                assert(referencedPoints.toSet().size == referencedPoints.size) {
                    "Deduped array $p contains duplicate points"
                }
            }

            // All three arrays should reference the same set of points
            val pointSets = (0 until 3).map { p ->
                dedupedArrays[p].map { points[it] }.toSet()
            }
            assert(pointSets[0] == pointSets[1] && pointSets[1] == pointSets[2]) {
                "Deduped arrays reference different point sets"
            }
        }
    }

    "toThreeDTree maintains the k-d tree property at each level" {
        val pointArb = Arb.bind(
            Arb.long(-1000L..1000L),
            Arb.long(-1000L..1000L),
            Arb.long(-1000L..1000L)
        ) { x, y, z -> Point(x, y, z) }

        val pointsArb = Arb.list(pointArb, 1..200)

        checkAll(pointsArb) { points: List<Point> ->
            val tree = points.toThreeDTree()

            // Verify k-d tree property: for each node at depth d,
            // all left descendants have coordinate[d%3] <= node's coordinate[d%3]
            // all right descendants have coordinate[d%3] >= node's coordinate[d%3]
            fun verifyKdProperty(node: ThreeDTree.Node?, depth: Int): Boolean {
                if (node == null) return true

                val dim = depth % 3
                fun getCoord(p: Point) = when (dim) {
                    0 -> p.x
                    1 -> p.y
                    else -> p.z
                }

                val nodeCoord = getCoord(node.data)

                // Check all points in left subtree are <= nodeCoord for this dimension
                fun allPointsInSubtree(n: ThreeDTree.Node?): List<Point> {
                    if (n == null) return emptyList()
                    return listOf(n.data) + allPointsInSubtree(n.left) + allPointsInSubtree(n.right)
                }

                val leftPoints = allPointsInSubtree(node.left)
                val rightPoints = allPointsInSubtree(node.right)

                for (p in leftPoints) {
                    assert(getCoord(p) <= nodeCoord) {
                        "k-d property violated: left point $p has coord ${getCoord(p)} > $nodeCoord at depth $depth (dim $dim)"
                    }
                }

                for (p in rightPoints) {
                    assert(getCoord(p) >= nodeCoord) {
                        "k-d property violated: right point $p has coord ${getCoord(p)} < $nodeCoord at depth $depth (dim $dim)"
                    }
                }

                return verifyKdProperty(node.left, depth + 1) && verifyKdProperty(node.right, depth + 1)
            }

            verifyKdProperty(tree.root, 0)
        }
    }

    "toThreeDTree produces a balanced tree with height <= ceil(lg(n))" {
        val pointArb = Arb.bind(
            Arb.long(-1000L..1000L),
            Arb.long(-1000L..1000L),
            Arb.long(-1000L..1000L)
        ) { x, y, z -> Point(x, y, z) }

        val pointsArb = Arb.list(pointArb, 1..200)

        checkAll(pointsArb) { points: List<Point> ->
            val tree = points.toThreeDTree()
            val uniqueCount = points.toSet().size

            if (uniqueCount == 0) return@checkAll

            // Compute tree height
            fun height(node: ThreeDTree.Node?): Int {
                if (node == null) return 0
                return 1 + maxOf(height(node.left), height(node.right))
            }

            val treeHeight = height(tree.root)

            // For a balanced tree, height should be <= ceil(log2(n)) + 1
            // (the +1 accounts for the root level)
            val maxAllowedHeight = kotlin.math.ceil(kotlin.math.log2(uniqueCount.toDouble())).toInt() + 1

            assert(treeHeight <= maxAllowedHeight) {
                "Tree not balanced: height $treeHeight > max allowed $maxAllowedHeight for $uniqueCount unique points"
            }
        }
    }

    "iterator returns all unique points exactly once" {
        val pointArb = Arb.bind(
            Arb.long(-1000L..1000L),
            Arb.long(-1000L..1000L),
            Arb.long(-1000L..1000L)
        ) { x, y, z -> Point(x, y, z) }

        val pointsArb = Arb.list(pointArb, 1..100)

        checkAll(pointsArb) { points: List<Point> ->
            val tree = points.toThreeDTree()
            val uniquePoints = points.toSet()

            // Collect all points from iterator
            val iteratedPoints = tree.toList()

            // Should have same count as unique points
            assert(iteratedPoints.size == uniquePoints.size) {
                "Iterator returned ${iteratedPoints.size} points, expected ${uniquePoints.size}"
            }

            // Should contain exactly the same points
            assert(iteratedPoints.toSet() == uniquePoints) {
                "Iterator returned different points than expected"
            }
        }
    }

    "nearestNeighbor returns the same result as brute force search" {
        val pointArb = Arb.bind(
            Arb.long(-1000L..1000L),
            Arb.long(-1000L..1000L),
            Arb.long(-1000L..1000L)
        ) { x, y, z -> Point(x, y, z) }

        val pointsArb = Arb.list(pointArb, 1..100)

        checkAll(pointsArb) { points: List<Point> ->
            val tree = points.toThreeDTree()
            val uniquePoints = points.toSet().toList()

            if (uniquePoints.isEmpty()) return@checkAll

            fun distSq(p1: Point, p2: Point): Long {
                val dx = p1.x - p2.x
                val dy = p1.y - p2.y
                val dz = p1.z - p2.z
                return dx * dx + dy * dy + dz * dz
            }

            // Test with a few random query points from the set
            val queryPoints = uniquePoints.take(minOf(5, uniquePoints.size))

            for (query in queryPoints) {
                // Brute force: sort all points by distance to query, with stable tie-breaking by point coordinates
                val sortedByDist = uniquePoints.sortedWith(compareBy({ distSq(query, it) }, { it.x }, { it.y }, { it.z }))

                // Test k = 0, 1, 2 (nearest, 2nd nearest, 3rd nearest)
                for (k in 0 until minOf(3, uniquePoints.size)) {
                    val kdResult = tree.nearestNeighbor(query, k)
                    val bruteResult = sortedByDist[k]

                    // Compare the actual points, not just distances
                    assert(kdResult == bruteResult) {
                        "k=$k nearest neighbor mismatch for query $query: " +
                        "k-d tree returned $kdResult (dist²=${kdResult?.let { distSq(query, it) }}), " +
                        "brute force returned $bruteResult (dist²=${distSq(query, bruteResult)})"
                    }
                }
            }
        }
    }
})
