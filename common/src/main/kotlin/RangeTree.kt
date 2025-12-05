class RangeTree {
	var root: Node? = null

	// Returns a boolean indicating whether or no
	// the tree already contained that value
	fun insert(n: Int): Boolean {
		if (root == null) {
			root = Node.withoutChildren(n)
			return false
		}

		return false
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

	// Returns the right child, which is the new root of the subtree
	// previously rooted at x.
	fun rightRotate(y: Node): Node {
		val x = y.left
		// This is just an assumption made by the algorithm
		assert(x != null)
		// But we still have to assert everywhere because Kotlin
		// is worried about concurrent modification.
		y.left = x!!.left
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

	companion object {
		fun empty(): RangeTree {}
	}

	class Node(
		var key: Int,
		var left: Node? = null,
		var right: Node? = null,
		var parent: Node? = null,
		var color: Color
	) {


		companion object {
			fun withoutChildren(k: Int): Node {
				return Node(k, null, null)
			}

		}
	}

	enum class Color {
		RED,
		BLACK
	}

	fun Node?.color(): Color {

	}
}