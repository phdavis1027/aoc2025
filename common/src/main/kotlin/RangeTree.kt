data class Interval(val low: Long, val high: Long) : Comparable<Interval> {
	override fun compareTo(other: Interval): Int = this.low.compareTo(other.low)

	fun overlaps(other: Interval): Boolean = this.low <= other.high && other.low <= this.high

	fun merge(other: Interval): Interval = Interval(minOf(this.low, other.low), maxOf(this.high, other.high))

	fun splitAt(n: Long): Pair<Interval?, Interval?> {
		if (n < low || n > high) {
			throw IndexOutOfBoundsException()
		}

		if (n == low) return Pair(null, Interval(low + 1, high))
		if (n == high) return Pair(Interval(low, high - 1), null)

		// n is strictly between low and high
		return Pair(
			Interval(low, n - 1),
			Interval(n + 1, high)
		)
	}
}

fun Long.overlaps(interval: Interval): Boolean = this >= interval.low && this <= interval.high

class RangeTree private constructor(val NIL: Node) {
	var root: Node = NIL

	constructor() : this(
		Node(
			interval = Interval(Long.MIN_VALUE, Long.MIN_VALUE),
			color = Color.BLACK,
			max = Long.MIN_VALUE
		).apply {
			left = this
			right = this
			parent = this
		}
	)

	fun bubble(node: Node): Node {
		val left = node.left
		val right = node.right
		assert(node.hasLeftChild())
		assert(node.hasRightChild())
		assert(left.isRed())
		assert(right.isRed())
		assert(node.isBlack())
		left.color = Color.BLACK
		right.color = Color.BLACK
		node.color = Color.RED
		return node
	}

	fun intervalSearch(value: Long): Node? = findOverlapping(Interval(value, value))

	fun findOverlapping(interval: Interval): Node? {
		var x = root
		while (!x.isNil()) {
			if (x.interval.overlaps(interval)) {
				return x
			}
			if (x.hasLeftChild() && x.left.max >= interval.low) {
				x = x.left
			} else {
				x = x.right
			}
		}
		return null
	}

	private fun propagateMax(from: Node) {
		var node = from
		while (!node.isNil()) {
			node.updateMax()
			node = node.parent
		}
	}

	fun insert(interval: Interval): Node {
		val overlapping = findOverlapping(interval)
		if (overlapping != null) {
			val merged = interval.merge(overlapping.interval)
			delete(overlapping)
			return insert(merged)
		}

		val z = Node.withoutChildren(interval, NIL)
		var y: Node = NIL
		var x = root
		while (!x.isNil()) {
			y = x
			if (z.interval < x.interval) {
				x = x.left
			} else {
				x = x.right
			}
		}
		z.parent = y
		if (y.isNil()) {
			root = z
		} else if (z.interval < y.interval) {
			y.left = z
		} else {
			y.right = z
		}
		z.color = Color.RED

		propagateMax(z.parent)

		return this.fixup(z)
	}

	private fun swapColor(x: Node, y: Node): Node {
		assert(x.color != y.color)
		if (x.color == Color.RED) {
			y.color = Color.RED
			x.color = Color.BLACK
			return y
		} else {
			y.color = Color.BLACK
			x.color = Color.RED
			return x
		}
	}

	private fun fixup(z_: Node): Node {
		fun RangeTree.case3Right(z: Node) {
			this.rightRotate(
				this.swapColor(
					z.parent,
					z.parent.parent
				)
			)
		}

		fun RangeTree.case3Left(z: Node) {
			this.leftRotate(
				this.swapColor(
					z.parent,
					z.parent.parent
				)
			)
		}

		var z = z_
		if (z.parent.isNil()) {
			assert(this.root == z)
		}
		while (z.parent.isRed()) {
			if (z.parent.isLeftChild()) {
				val y = z.parent.parent.right
				if (y.isRed()) {
					z = this.bubble(z.parent.parent)
				} else {
					if (z.isRightChild()) {
						z = z.parent
						this.leftRotate(z)
					}
					this.case3Right(z)
				}

			} else {
				val y = z.parent.parent.left
				if (y.isRed()) {
					z = this.bubble(z.parent.parent)
				} else if (z == z.parent.left) {
					z = z.parent
					this.rightRotate(z)
					this.case3Left(z)
				} else {
					this.case3Left(z)
				}
			}
		}

		root.color = Color.BLACK
		return z
	}


	fun leftRotate(parent: Node): Node {
		val rightChild = parent.right
		parent.right = rightChild.left
		if (rightChild.hasLeftChild()) {
			rightChild.left.parent = parent
		}
		rightChild.parent = parent.parent

		if (parent.parent.isNil()) {
			this.root = rightChild
		} else if (parent.isLeftChild()) {
			parent.parent.left = rightChild
		} else {
			parent.parent.right = rightChild
		}
		rightChild.left = parent
		parent.parent = rightChild

		parent.updateMax()
		rightChild.updateMax()

		return rightChild
	}

	fun rightRotate(parent: Node): Node {
		val leftChild = parent.left
		assert(parent.hasLeftChild())
		parent.left = leftChild.right
		if (leftChild.hasRightChild()) {
			leftChild.right.parent = parent
		}
		leftChild.parent = parent.parent
		if (leftChild.parent.isNil()) {
			this.root = leftChild
		} else if (parent.isLeftChild()) {
			parent.parent.left = leftChild
		} else {
			parent.parent.right = leftChild
		}
		leftChild.right = parent
		parent.parent = leftChild

		parent.updateMax()
		leftChild.updateMax()

		return leftChild
	}

	fun print() {
		if (root.isNil()) {
			println("(empty tree)")
			return
		}
		printNode(root, "", true)
	}

	private fun printNode(node: Node, prefix: String, isLast: Boolean) {
		if (node.isNil()) return
		val colorChar = if (node.color == Color.RED) "R" else "B"
		println("$prefix${if (isLast) "└── " else "├── "}[${node.interval.low}, ${node.interval.high}]($colorChar) max=${node.max}")
		val childPrefix = prefix + if (isLast) "    " else "│   "
		printNode(node.left, childPrefix, !node.hasRightChild())
		printNode(node.right, childPrefix, true)
	}

	companion object {
		fun empty(): RangeTree = RangeTree()
	}

	class Node(
		var interval: Interval,
		var color: Color,
		var max: Long
	) {
		lateinit var left: Node
		lateinit var right: Node
		lateinit var parent: Node

		fun isLeftChild() = this == this.parent.left
		fun isRightChild() = this == this.parent.right

		companion object {
			fun withoutChildren(interval: Interval, nil: Node): Node {
				return Node(interval, Color.RED, interval.high).apply {
					left = nil
					right = nil
					parent = nil
				}
			}
		}
	}

	enum class Color(i: Int) {
		RED(0),
		BLACK(1)
	}

	fun delete(toDelete: Node) {
		var replaced = toDelete
		var replacedOriginalColor = replaced.color
		var replacer: Node = toDelete.left
		var fixupStart: Node = NIL
		if (!toDelete.hasLeftChild()) {
			replacer = toDelete.right
			this.transplant(toDelete, replacer)
			fixupStart = replacer.parent
		} else if (!toDelete.hasRightChild()) {
			replacer = toDelete.left
			this.transplant(toDelete, replacer)
			fixupStart = replacer.parent
		} else {
			replaced = this.successor(toDelete)
			replacedOriginalColor = replaced.color
			replacer = replaced.right
			if (replaced.parent == toDelete) {
				replacer.parent = replaced
				fixupStart = replaced
			} else {
				fixupStart = replaced.parent
				this.transplant(replaced, replaced.right)
				replaced.right = toDelete.right
				replaced.right.parent = replaced
			}
			this.transplant(toDelete, replaced)
			replaced.left = toDelete.left
			toDelete.left.parent = replaced
			replaced.color = toDelete.color
			replaced.updateMax()
		}
		propagateMax(fixupStart)
		if (replacedOriginalColor == Color.BLACK) {
			deleteFixup(replacer)
		}
	}

	fun minimum(node: Node): Node {
		var answer: Node = node
		while (answer.hasLeftChild()) {
			answer = answer.left
		}
		return answer
	}

	fun successor(node: Node): Node = this.minimum(node.right)

	private fun deleteFixup(x: Node) {
		var bubble: Node = x
		fun case3Left(w: Node) {
			w.color = bubble.parent.color
			bubble.parent.color = Color.BLACK
			w.right.color = Color.BLACK
			this.leftRotate(bubble.parent)
			bubble = this.root
		}

		fun case3Right(w: Node) {
			w.color = bubble.parent.color
			bubble.parent.color = Color.BLACK
			w.left.color = Color.BLACK
			this.rightRotate(bubble.parent)
			bubble = this.root
		}
		while (bubble != this.root && bubble.isBlack()) {
			if (bubble.isLeftChild()) {
				var rightSibling = bubble.parent.right
				if (rightSibling.isRed()) {
					rightSibling.color = Color.BLACK
					bubble.parent.color = Color.RED
					this.leftRotate(bubble.parent)
					rightSibling = bubble.parent.right
				}
				if (rightSibling.left.isBlack() && rightSibling.right.isBlack()) {
					rightSibling.color = Color.RED
					bubble = bubble.parent
				} else {
					if (rightSibling.right.isBlack()) {
						rightSibling.left.color = Color.BLACK
						rightSibling.color = Color.RED
						this.rightRotate(rightSibling)
						rightSibling = bubble.parent.right
					}
					case3Left(rightSibling)
				}
			} else {
				var leftSibling = bubble.parent.left
				if (leftSibling.isRed()) {
					leftSibling.color = Color.BLACK
					bubble.parent.color = Color.RED
					this.rightRotate(bubble.parent)
					leftSibling = bubble.parent.left
				}
				if (leftSibling.right.isBlack() && leftSibling.left.isBlack()) {
					leftSibling.color = Color.RED
					bubble = bubble.parent
				} else {
					if (leftSibling.left.isBlack()) {
						leftSibling.right.color = Color.BLACK
						leftSibling.color = Color.RED
						this.leftRotate(leftSibling)
						leftSibling = bubble.parent.left
					}
					case3Right(leftSibling)
				}
			}
		}
		bubble.color = Color.BLACK
	}

	private fun transplant(u: Node, v: Node) {
		if (u.parent.isNil()) {
			this.root = v
		} else if (u.isLeftChild()) {
			u.parent.left = v
		} else {
			u.parent.right = v
		}
		v.parent = u.parent
	}

	fun sumLengths(): Long = sumLengths(root)

	private fun sumLengths(node: Node): Long {
		if (node.isNil()) return 0L
		val length = node.interval.high - node.interval.low + 1
		return length + sumLengths(node.left) + sumLengths(node.right)
	}

	fun Node.isNil() = this == NIL
	fun Node.hasLeftChild() = !this.left.isNil()
	fun Node.hasRightChild() = !this.right.isNil()
	fun Node.isRed() = this.color == Color.RED
	fun Node.isBlack() = this.color == Color.BLACK
	fun Node.updateMax() {
		max = maxOf(
			interval.high,
			if (left.isNil()) Long.MIN_VALUE else left.max,
			if (right.isNil()) Long.MIN_VALUE else right.max
		)
	}
}