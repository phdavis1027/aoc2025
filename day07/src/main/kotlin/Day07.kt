import java.lang.foreign.*
import java.lang.invoke.MethodHandle

object Native {
    private val linker: Linker = Linker.nativeLinker()
    val arena: Arena = Arena.global()

    private val lib: SymbolLookup by lazy {
        val libPath = System.getProperty("java.library.path")
        val os = System.getProperty("os.name").lowercase()
        val libName = when {
            os.contains("linux") -> "libnative.so"
            os.contains("mac") -> "libnative.dylib"
            else -> throw UnsupportedOperationException("Unsupported OS")
        }
        SymbolLookup.libraryLookup("$libPath/$libName", arena)
    }

    private fun lookup(name: String): MemorySegment =
        lib.find(name).orElseThrow { NoSuchElementException("Symbol not found: $name") }

    // int32_t open_file(const char* path)
    private val openFileHandle: MethodHandle = linker.downcallHandle(
        lookup("open_file"),
        FunctionDescriptor.of(ValueLayout.JAVA_INT, ValueLayout.ADDRESS)
    )

    // int64_t read_line(char* buf, int64_t buf_len)
    private val readLineHandle: MethodHandle = linker.downcallHandle(
        lookup("read_line"),
        FunctionDescriptor.of(ValueLayout.JAVA_LONG, ValueLayout.ADDRESS, ValueLayout.JAVA_LONG)
    )

    // void close_file(void)
    private val closeFileHandle: MethodHandle = linker.downcallHandle(
        lookup("close_file"),
        FunctionDescriptor.ofVoid()
    )

    private val initScanVecsHandle: MethodHandle = linker.downcallHandle(
        lookup("init_scan_vecs"),
        FunctionDescriptor.ofVoid()
    )

    private val scanLinePairPartOneHandle: MethodHandle = linker.downcallHandle(
        lookup("scan_line_pair_part_one"),
        FunctionDescriptor.of(
            ValueLayout.JAVA_INT,
            ValueLayout.ADDRESS,
            ValueLayout.ADDRESS,
            ValueLayout.JAVA_LONG,
            ValueLayout.ADDRESS,
            ValueLayout.ADDRESS
        )
    )

    private val scanLinePairPartTwoHandle: MethodHandle = linker.downcallHandle(
        lookup("scan_line_pair_part_two"),
        FunctionDescriptor.of(
            ValueLayout.JAVA_LONG,
            ValueLayout.ADDRESS,
            ValueLayout.ADDRESS,
            ValueLayout.JAVA_LONG,
            ValueLayout.ADDRESS,
            ValueLayout.ADDRESS
        )
    )

    fun openFile(path: String): Boolean {
        val pathSegment = arena.allocateUtf8String(path)
        return (openFileHandle.invokeExact(pathSegment) as Int) == 0
    }

    fun readLine(buf: MemorySegment, bufLen: Long): Long =
        readLineHandle.invokeExact(buf, bufLen) as Long

    fun closeFile() {
        closeFileHandle.invokeExact()
    }

    fun initScanVecs() {
        initScanVecsHandle.invokeExact()
    }

    fun scanLinePairPartOne(
        prevBuf: MemorySegment,
        currBuf: MemorySegment,
        len: Long,
        outPipeMask: MemorySegment,
        outCaretMask: MemorySegment
    ): Int = scanLinePairPartOneHandle.invokeExact(prevBuf, currBuf, len, outPipeMask, outCaretMask) as Int

    fun scanLinePairPartTwo(
        prevBuf: MemorySegment,
        currBuf: MemorySegment,
        len: Long,
        prevPaths: MemorySegment,
        currPaths: MemorySegment
    ): Long = scanLinePairPartTwoHandle.invokeExact(prevBuf, currBuf, len, prevPaths, currPaths) as Long
}

inline fun <T> readLines(path: String, lineLength: Long, block: (Sequence<MemorySegment>) -> T): T {
    check(Native.openFile(path)) { "Failed to open $path" }
    val bufLen = lineLength + 2
    Arena.ofConfined().use { lineArena ->
        val buf = lineArena.allocate(bufLen)
        val seq = sequence {
            while (true) {
                val len = Native.readLine(buf, bufLen)
                if (len < 0) break
                yield(buf.asSlice(0, len))
            }
        }
        try {
            return block(seq)
        } finally {
            Native.closeFile()
        }
    }
}

// Allocate buffer padded to 256 bytes for safe SIMD writes beyond lineLen
private fun Arena.allocatePaddedBuf(lineLength: Long): MemorySegment =
    allocate(maxOf(lineLength + 2, 256L))

fun bufToString(buf: MemorySegment, len: Long) =
	String(buf.asSlice(0, len).toArray(ValueLayout.JAVA_BYTE))

class TestCasePartOne {
    companion object {
        @JvmStatic
        fun main(args: Array<String>) {
            val path = "day07/src/test.txt"
            val lineLength = 15L
            check(Native.openFile(path)) { "Failed to open $path" }
            Native.initScanVecs()
            val bufLen = lineLength + 2

            Arena.ofConfined().use { arena ->
                var currBuf = arena.allocatePaddedBuf(lineLength)
                var prevBuf = arena.allocatePaddedBuf(lineLength)
                val pipeMask = arena.allocate(ValueLayout.JAVA_LONG, 4)
                val caretMask = arena.allocate(ValueLayout.JAVA_LONG, 4)

                var totalSplits = 0
                var lineNum = 0
                try {
                    val firstLen = Native.readLine(prevBuf, bufLen)
                    if (firstLen < 0) return
                    for (i in 0 until firstLen) {
                        if (prevBuf.get(ValueLayout.JAVA_BYTE, i) == 'S'.code.toByte()) {
                            prevBuf.set(ValueLayout.JAVA_BYTE, i, '|'.code.toByte())
                            break
                        }
                    }
                    // println("${bufToString(prevBuf, firstLen)}")
                    System.out.flush()

                    while (true) {
                        val len = Native.readLine(currBuf, bufLen)
                        if (len < 0) break
                        lineNum++
                        val splits = Native.scanLinePairPartOne(prevBuf, currBuf, len, pipeMask, caretMask)
                        totalSplits += splits
                        // println("${bufToString(currBuf, len)} (aligned: $splits, total: $totalSplits)")
                        System.out.flush()
                        val tmp = prevBuf
                        prevBuf = currBuf
                        currBuf = tmp
                    }
                    println("\nFinal count: $totalSplits")
                } finally {
                    Native.closeFile()
                }
            }
        }
    }
}

class SolvePartOne {
    companion object {
        @JvmStatic
        fun main(args: Array<String>) {
            val path = "day07/src/input.txt"
            val lineLength = 141L
            check(Native.openFile(path)) { "Failed to open $path" }
            Native.initScanVecs()
            val bufLen = lineLength + 2

            Arena.ofConfined().use { arena ->
                var currBuf = arena.allocatePaddedBuf(lineLength)
                var prevBuf = arena.allocatePaddedBuf(lineLength)
                val pipeMask = arena.allocate(ValueLayout.JAVA_LONG, 4)
                val caretMask = arena.allocate(ValueLayout.JAVA_LONG, 4)

                var totalSplits = 0
                var lineNum = 0
                try {
                    val firstLen = Native.readLine(prevBuf, bufLen)
                    if (firstLen < 0) {
						println("Failed to a line, error $firstLen")
						return
					}
                    for (i in 0 until firstLen) {
                        if (prevBuf.get(ValueLayout.JAVA_BYTE, i) == 'S'.code.toByte()) {
                            prevBuf.set(ValueLayout.JAVA_BYTE, i, '|'.code.toByte())
                            break
                        }
                    }

                    while (true) {
                        val len = Native.readLine(currBuf, bufLen)
                        if (len < 0) break
                        lineNum++
                        val splits = Native.scanLinePairPartOne(prevBuf, currBuf, len, pipeMask, caretMask)
                        totalSplits += splits 
                        val tmp = prevBuf
                        prevBuf = currBuf
                        currBuf = tmp
                    }
                    println("\nFinal count: $totalSplits")
                } finally {
                    Native.closeFile()
                }
            }
        }
    }
}

// Part Two
class TestCasePartTwo {
    companion object {
        @JvmStatic
        fun main(args: Array<String>) {
            val path = "day07/src/test.txt"
            val lineLength = 15L
            check(Native.openFile(path)) { "Failed to open $path" }
            Native.initScanVecs()
            val bufLen = lineLength + 2

            Arena.ofConfined().use { arena ->
                var currBuf = arena.allocatePaddedBuf(lineLength)
                var prevBuf = arena.allocatePaddedBuf(lineLength)
                // Path counts: one int64 per column position
                var prevPaths = arena.allocate(lineLength * 8)
                var currPaths = arena.allocate(lineLength * 8)

                var lineNum = 0
                try {
                    val firstLen = Native.readLine(prevBuf, bufLen)
                    if (firstLen < 0) return
                    // Find S and initialize path count
                    for (i in 0 until firstLen) {
                        if (prevBuf.get(ValueLayout.JAVA_BYTE, i) == 'S'.code.toByte()) {
                            prevBuf.set(ValueLayout.JAVA_BYTE, i, '|'.code.toByte())
                            prevPaths.setAtIndex(ValueLayout.JAVA_LONG, i, 1L)
                            break
                        }
                    }

                    while (true) {
                        val len = Native.readLine(currBuf, bufLen)
                        if (len < 0) break
                        lineNum++
                        Native.scanLinePairPartTwo(prevBuf, currBuf, len, prevPaths, currPaths)
                        // Swap buffers and path arrays
                        val tmpBuf = prevBuf
                        prevBuf = currBuf
                        currBuf = tmpBuf
                        val tmpPaths = prevPaths
                        prevPaths = currPaths
                        currPaths = tmpPaths
                    }
                    // Sum all remaining paths
                    var finalCount = 0L
                    for (i in 0 until lineLength) {
                        finalCount += prevPaths.getAtIndex(ValueLayout.JAVA_LONG, i)
                    }
                    println("\nFinal count: $finalCount")
                } finally {
                    Native.closeFile()
                }
            }
        }
    }
}

class SolvePartTwo {
    companion object {
        @JvmStatic
        fun main(args: Array<String>) {
            val path = "day07/src/input.txt"
            val lineLength = 141L
            check(Native.openFile(path)) { "Failed to open $path" }
            Native.initScanVecs()
            val bufLen = lineLength + 2

            Arena.ofConfined().use { arena ->
                var currBuf = arena.allocatePaddedBuf(lineLength)
                var prevBuf = arena.allocatePaddedBuf(lineLength)
                var prevPaths = arena.allocate(lineLength * 8)
                var currPaths = arena.allocate(lineLength * 8)

                try {
                    val firstLen = Native.readLine(prevBuf, bufLen)
                    if (firstLen < 0) {
                        println("Failed to read a line")
                        return
                    }
                    for (i in 0 until firstLen) {
                        if (prevBuf.get(ValueLayout.JAVA_BYTE, i) == 'S'.code.toByte()) {
                            prevBuf.set(ValueLayout.JAVA_BYTE, i, '|'.code.toByte())
                            prevPaths.setAtIndex(ValueLayout.JAVA_LONG, i, 1L)
                            break
                        }
                    }

                    while (true) {
                        val len = Native.readLine(currBuf, bufLen)
                        if (len < 0) break
                        Native.scanLinePairPartTwo(prevBuf, currBuf, len, prevPaths, currPaths)
                        val tmpBuf = prevBuf
                        prevBuf = currBuf
                        currBuf = tmpBuf
                        val tmpPaths = prevPaths
                        prevPaths = currPaths
                        currPaths = tmpPaths
                    }
                    var finalCount = 0L
                    for (i in 0 until lineLength) {
                        finalCount += prevPaths.getAtIndex(ValueLayout.JAVA_LONG, i)
                    }
                    println("\nFinal count: $finalCount")
                } finally {
                    Native.closeFile()
                }
            }
        }
    }
}
