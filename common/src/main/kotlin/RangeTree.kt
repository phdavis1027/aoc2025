data class Interval(val low: Long, val high: Long) : Comparable<Interval> {
	override fun compareTo(other: Interval): Int = this.low.compareTo(other.low)

	fun splitAt(n: Long): Pair<Interval?, Interval?> {
		if (n < low || n > high) {
			throw IndexOutOfBoundsException()
		}

		if (n == low) return Pair(null, this)
		if (n == high) return Pair(this, null)

		return Pair(
			Interval(low, n),
			Interval(n, high)
		)
	}
}

fun Long.overlaps(interval: Interval): Boolean = this >= interval.low && this <= interval.high

class RangeTree {
	var root: Node? = null

	// Returns the inserted node
	fun insert(interval: Interval): Node {
		var z = Node.withoutChildren(interval)
		// Represents the insertion point
		var y: Node? = null
		var x = root
		// Find the insertion point
		while (x != null) {
			y = x
			if (z.interval < x.interval) {
				x = x.left
			} else {
				x = x.right
			}
		}
		z.parent = y
		if (y == null) {
			// If the insertion point is null,
			// then the tree was empty, and the inserted node
			// is now the root.
			// As a nice coincidence, the previous line
			// would also correctly set the parent to null.
			root = z
		} else if (z.interval < y.interval) {
			y.left = z
		} else {
			y.right = z
		}
		z.color = Color.RED

		return this.fixup(z)
	}

	private fun fixup(z_: Node): Node {
		fun RangeTree.case3Right(z: Node) {
			z.parent!!.color = Color.BLACK
			z.parent!!.parent!!.color = Color.RED
			this.rightRotate(z.parent!!.parent!!)
		}
		fun RangeTree.case3Left(z: Node) {
			z.parent!!.color = Color.BLACK
			z.parent!!.parent!!.color = Color.RED
			this.leftRotate(z.parent!!.parent!!)
		}
		var z = z_
		if (z.parent == null) {
			assert(this.root == z)

		}
		while (z.parent?.color() == Color.RED) {
			// z.parent is not null, since null.color() == Color.BLACK
			if (z.parent == z.parent!!.parent!!.left) {
				// From CLRS:
				// the while loop maintains these invariants:
				// a) z is red
				// b) if z.parent is the root, then z.parent is black
				// c) "If the tree violates any of the red-black properties, then it violates at most
				// one of them, and the violation is of either property 2 or property 4. If the
				// tree violates property 2, it is because ´ is the root and is red. If the tree
				// violates property 4, it is because both ´ and ´:p are red.
				// ---
				// ^^^ these are proven in the subsequent text, but
				// just trust me, bro.
				// Anyway, by (b), as well as the fact that
				// we enter the loop iteration only if z.p is red,
				// we know that z.p is not the root.
				var y = z.parent!!.parent!!.right
				if (y.color() == Color.RED) {
					// y is not null, since null.color() == Color.BLACK
					z.parent!!.color = Color.BLACK
					y!!.color = Color.BLACK
					z.parent!!.parent!!.color = Color.RED
					z = z.parent!!.parent!!
				} else if (z == z.parent!!.right) {
					z = z.parent!!
					this.leftRotate(z)
					this.case3Right(z)
				} else {
					this.case3Right(z)
				}
			} else {
				var y = z.parent!!.parent!!.left
				if (y.color() == Color.RED) {
					// y is not null, since null.color() == Color.BLACK
					z.parent!!.color = Color.BLACK
					y!!.color = Color.BLACK
					z.parent!!.parent!!.color = Color.RED
					z = z.parent!!.parent!!
				} else if (z == z.parent!!.left) {
					z = z.parent!!
					this.rightRotate(z)
					this.case3Left(z)
				} else {
					this.case3Left(z)
				}
			}
		}

		// SAFETY: `fixup` is only called after operations
		// which leave the tree non-empty like `insert`
		root!!.color = Color.BLACK
		return z
	}

	// Returns the right child, which is the new root of the subtree
	// previously rooted at x.
	fun leftRotate(x: Node): Node {
		val y = x.right
		// This is just an assumption made by the algorithm
		assert(y != null)
		// But we still have to assert everywhere because Kotlin
		// is worried about concurrent modification.
		x.right = y!!.left
		// Tell y's child about their new parent
		if (y.left != null) {
			y.left!!.parent = x
		}
		// Tell y's parent about their new child
		y.parent = x.parent
		// Only the root has a null parent.
		// This is the one case where we
		// have to modify the tree itself.
		if (x.parent == null) {
			this.root = y
		} else if (x == x.parent!!.left) {
			// Tell x's parent about their new child
			x.parent!!.left = y
		} else { x.parent!!.right = y }
		y.left = x
		x.parent = y

		return y
	}

	// Returns the left child, which is the new root of the subtree
	// previously rooted at y.
	fun rightRotate(y: Node): Node {
		val x = y.left
		// This is just an assumption made by the algorithm
		assert(x != null)
		// But we still have to assert everywhere because Kotlin
		// is worried about concurrent modification.
		y.left = x!!.right
		// Tell x's child about their new parent
		if (x.right != null) {
			x.right!!.parent = y
		}
		// Tell x's parent about their new child
		x.parent = y.parent
		// Only the root has a null parent.
		// This is the one case where we
		// have to modify the tree itself.
		if (y.parent == null) {
			this.root = x
		} else if (y == y.parent!!.left) { // Is x a left child?
			// Tell x's parent about their new child
			y.parent!!.left = x
		} else { y.parent!!.right = x }
		x.right= y
		y.parent = x

		return x
	}

	fun print() {
		if (root == null) {
			println("(empty tree)")
			return
		}
		printNode(root, "", true)
	}

	private fun printNode(node: Node?, prefix: String, isLast: Boolean) {
		if (node == null) return
		val colorChar = if (node.color == Color.RED) "R" else "B"
		println("$prefix${if (isLast) "└── " else "├── "}[${node.interval.low}, ${node.interval.high}]($colorChar)")
		val childPrefix = prefix + if (isLast) "    " else "│   "
		printNode(node.left, childPrefix, node.right == null)
		printNode(node.right, childPrefix, true)
	}

	companion object {
		fun empty(): RangeTree = RangeTree()
	}

	class Node(
		var interval: Interval,
		var left: Node? = null,
		var right: Node? = null,
		var parent: Node? = null,
		var color: Color
	) {


		companion object {
			fun withoutChildren(interval: Interval): Node {
				return Node(interval, null, null, null, Color.RED)
			}
		}
	}

	enum class Color {
		RED,
		BLACK
	}

	fun Node?.color(): Color = this?.color ?: Color.BLACK
	fun Node?.setColor(color: Color) {
		if (this != null) {
			this.color = color
		}
		assert(color == Color.BLACK)
	}
}

fun main() {
	println("Testing RangeTree insert:")
	val tree = RangeTree.empty()

	// Insert intervals and print after each
	val intervals = listOf(
		Interval(10, 15),
		Interval(5, 8),
		Interval(15, 23),
		Interval(3, 6),
		Interval(7, 10),
		Interval(12, 14),
		Interval(20, 25)
	)
	for (interval in intervals) {
		println("\nInserting [${interval.low}, ${interval.high}]:")
		tree.insert(interval)
		tree.print()
	}

	// Verify in-order traversal gives sorted order by low endpoint
	println("\n\nIn-order traversal (should be sorted by low endpoint):")
	fun inOrder(node: RangeTree.Node?) {
		if (node == null) return
		inOrder(node.left)
		print("[${node.interval.low}, ${node.interval.high}] ")
		inOrder(node.right)
	}
	inOrder(tree.root)
	println()

	// Check that root is black (red-black property)
	println("\nRoot color is BLACK: ${tree.root?.color == RangeTree.Color.BLACK}")
}