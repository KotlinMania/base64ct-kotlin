// port-lint: source src/encoder.rs
package io.github.kotlinmania.base64ct.encoder

import io.github.kotlinmania.base64ct.ByteSlice
import io.github.kotlinmania.base64ct.EncodeResult
import io.github.kotlinmania.base64ct.EncoderResult
import io.github.kotlinmania.base64ct.FinishResult
import io.github.kotlinmania.base64ct.MIN_LINE_WIDTH
import io.github.kotlinmania.base64ct.OperationResult
import io.github.kotlinmania.base64ct.ReadResult
import io.github.kotlinmania.base64ct.asSlice
import io.github.kotlinmania.base64ct.checkedAdd
import io.github.kotlinmania.base64ct.checkedSub
import io.github.kotlinmania.base64ct.encoding.Encoding
import io.github.kotlinmania.base64ct.encoding.encodeSlice
import io.github.kotlinmania.base64ct.errors.Error
import io.github.kotlinmania.base64ct.fail
import io.github.kotlinmania.base64ct.lineending.LineEnding
import io.github.kotlinmania.base64ct.result

/** Stateful Base64 encoder with buffered, incremental encoding. */
// generic by design: E selects the alphabet used for every incremental encoding operation.
class Encoder<E : Encoding> private constructor(
    private val output: ByteSlice,
    private val encoding: E,
    private val lineWrapper: LineWrapper?,
) {
    private var cursor = 0
    private val blockBuffer = BlockBuffer()
    private var finished = false

    companion object {
        fun <E : Encoding> new(output: ByteArray, encoding: E): EncoderResult<E> =
            result(::EncoderResult) {
                if (output.isEmpty()) fail(Error.InvalidLength)
                Encoder(output.asSlice(), encoding, null)
            }

        fun <E : Encoding> newWrapped(
            output: ByteArray,
            width: Int,
            ending: LineEnding,
            encoding: E,
        ): EncoderResult<E> =
            result(::EncoderResult) {
                if (output.isEmpty()) fail(Error.InvalidLength)
                Encoder(output.asSlice(), encoding, LineWrapper.new(width, ending))
            }
    }

    fun encode(input: ByteArray): OperationResult = result(::OperationResult) { encodeInput(input.asSlice()) }

    private fun encodeInput(source: ByteSlice) {
        if (finished) fail(Error.InvalidLength)
        var input = source
        if (!blockBuffer.isEmpty()) input = processBuffer(input)
        while (!input.isEmpty()) {
            val inBlocks = input.size / 3
            val outBlocks = remaining().size / 4
            var blocks = minOf(inBlocks, outBlocks)
            blocks = lineWrapper?.wrapBlocks(blocks) ?: blocks
            if (blocks > 0) {
                val len = blocks * 3
                val aligned = input.slice(0, len)
                input = input.slice(len)
                performEncode(aligned)
            }
            if (!input.isEmpty()) input = processBuffer(input)
        }
    }

    fun position(): Int = cursor

    fun finish(): EncodeResult = result(::EncodeResult) { finishOutput().base64 }

    fun finishWithRemaining(): FinishResult = result(::FinishResult) { finishOutput() }

    private fun finishOutput(): EncodedOutput {
        if (finished) fail(Error.InvalidLength)
        finished = true
        if (!blockBuffer.isEmpty()) {
            val bufferLen = blockBuffer.position
            performEncode(blockBuffer.bytes.asSlice().slice(0, bufferLen))
        }
        return EncodedOutput(output.slice(0, cursor).text(), output.slice(cursor))
    }

    private fun remaining(): ByteSlice = output.slice(cursor)

    private fun processBuffer(input: ByteSlice): ByteSlice {
        val rest = blockBuffer.fill(input)
        if (blockBuffer.isFull()) performEncode(blockBuffer.take().asSlice())
        return rest
    }

    private fun performEncode(input: ByteSlice): Int {
        var len = encoding.encodeSlice(input, remaining()).size
        len = lineWrapper?.insertNewlines(output.slice(cursor), len) ?: len
        cursor = checkedAdd(cursor, len)
        return len
    }

    fun write(buf: ByteArray): ReadResult =
        result(::ReadResult) {
            encodeInput(buf.asSlice())
            buf.size
        }

    /** Flushing does not finalize a partial Base64 block. */
    fun flush(): OperationResult =
        result(::OperationResult) {
            if (finished) fail(Error.InvalidLength)
        }
}

/** Encoded text and a writable view of the unused output capacity. */
class EncodedOutput internal constructor(
    val base64: String,
    val remaining: ByteSlice,
)

private class BlockBuffer {
    var bytes = ByteArray(3)
        private set
    var position = 0
        private set

    fun fill(input: ByteSlice): ByteSlice {
        val remaining = checkedSub(3, position)
        val len = minOf(input.size, remaining)
        input.slice(0, len).copyTo(bytes.asSlice().slice(position))
        position = checkedAdd(position, len)
        return input.slice(len)
    }

    fun take(): ByteArray {
        check(isFull())
        val result = bytes
        bytes = ByteArray(3)
        position = 0
        return result
    }

    fun isEmpty(): Boolean = position == 0

    fun isFull(): Boolean = position == 3
}

private class LineWrapper(
    private var remaining: Int,
    private val width: Int,
    private val ending: LineEnding,
) {
    companion object {
        fun new(width: Int, ending: LineEnding): LineWrapper {
            if (width < MIN_LINE_WIDTH) fail(Error.InvalidLength)
            return LineWrapper(width, width, ending)
        }
    }

    fun wrapBlocks(blocks: Int): Int = if (blocks * 4 >= remaining) remaining / 4 else blocks

    fun insertNewlines(output: ByteSlice, length: Int): Int {
        if (length <= remaining) {
            remaining = checkedSub(remaining, length)
            return length
        }
        val buffer = output.slice(remaining)
        val bufferLen = checkedSub(length, remaining)
        check(bufferLen <= 4)
        val bufferEnd = checkedAdd(bufferLen, ending.len())
        if (bufferEnd >= buffer.size) fail(Error.InvalidLength)
        for (i in bufferLen - 1 downTo 0) buffer[checkedAdd(i, ending.len())] = buffer[i]
        ending.asBytes().asSlice().copyTo(buffer)
        remaining = checkedSub(width, bufferLen)
        return checkedAdd(length, ending.len())
    }
}
