package io.github.kotlinmania.base64ct

/** A bounded view of a byte buffer. Writes are visible in the original buffer. */
class ByteSlice(
    private val buffer: ByteArray,
    private val offset: Int = 0,
    val size: Int = buffer.size - offset,
) {
    init {
        require(offset >= 0 && size >= 0 && offset <= buffer.size - size)
    }

    operator fun get(index: Int): Byte {
        require(index in 0 until size)
        return buffer[offset + index]
    }

    operator fun set(index: Int, value: Byte) {
        require(index in 0 until size)
        buffer[offset + index] = value
    }

    fun toByteArray(): ByteArray = buffer.copyOfRange(offset, offset + size)

    internal fun slice(start: Int, end: Int = size): ByteSlice {
        require(start >= 0 && end >= start && end <= size)
        return ByteSlice(buffer, offset + start, end - start)
    }

    internal fun copyTo(destination: ByteSlice) {
        require(size <= destination.size)
        buffer.copyInto(destination.buffer, destination.offset, offset, offset + size)
    }

    internal fun text(): String = buffer.decodeToString(offset, offset + size, throwOnInvalidSequence = true)

    internal fun isEmpty(): Boolean = size == 0
}

internal fun ByteArray.asSlice(): ByteSlice = ByteSlice(this)

internal const val MIN_LINE_WIDTH: Int = 4
