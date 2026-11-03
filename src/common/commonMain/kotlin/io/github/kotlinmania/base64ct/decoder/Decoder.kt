// port-lint: source src/decoder.rs
package io.github.kotlinmania.base64ct.decoder

import io.github.kotlinmania.base64ct.ByteSlice
import io.github.kotlinmania.base64ct.DecodeResult
import io.github.kotlinmania.base64ct.DecodeVectorResult
import io.github.kotlinmania.base64ct.DecoderResult
import io.github.kotlinmania.base64ct.MIN_LINE_WIDTH
import io.github.kotlinmania.base64ct.OperationResult
import io.github.kotlinmania.base64ct.ReadResult
import io.github.kotlinmania.base64ct.asSlice
import io.github.kotlinmania.base64ct.checkedAdd
import io.github.kotlinmania.base64ct.checkedSub
import io.github.kotlinmania.base64ct.encoding.Encoding
import io.github.kotlinmania.base64ct.encoding.decodeSlice
import io.github.kotlinmania.base64ct.encoding.decodedLen
import io.github.kotlinmania.base64ct.errors.Error
import io.github.kotlinmania.base64ct.fail
import io.github.kotlinmania.base64ct.lineending.CHAR_CR
import io.github.kotlinmania.base64ct.lineending.CHAR_LF
import io.github.kotlinmania.base64ct.result

/** Stateful Base64 decoder with buffered, incremental decoding. */
// generic by design: E selects the alphabet retained by this decoder and its clones.
class Decoder<E : Encoding> private constructor(
    private var line: Line,
    private val lineReader: LineReader,
    private var remainingLength: Int,
    private val blockBuffer: BlockBuffer,
    private val encoding: E,
) {
    companion object {
        fun <E : Encoding> new(input: ByteArray, encoding: E): DecoderResult<E> =
            result(::DecoderResult) {
                val reader = LineReader.newUnwrapped(input.asSlice())
                Decoder(Line(), reader, reader.decodedLen(encoding), BlockBuffer(), encoding)
            }

        fun <E : Encoding> newWrapped(input: ByteArray, lineWidth: Int, encoding: E): DecoderResult<E> =
            result(::DecoderResult) {
                val reader = LineReader.newWrapped(input.asSlice(), lineWidth)
                Decoder(Line(), reader, reader.decodedLen(encoding), BlockBuffer(), encoding)
            }
    }

    /** Fill the complete output buffer with decoded data. */
    fun decode(out: ByteArray): DecodeResult = result(::DecodeResult) { decodeInto(out.asSlice()) }

    private fun decodeInto(out: ByteSlice): ByteSlice {
        if (isFinished() && !out.isEmpty()) fail(Error.InvalidLength)
        var outPos = 0
        while (outPos < out.size) {
            if (!blockBuffer.isEmpty()) {
                val bytes = blockBuffer.take(checkedSub(out.size, outPos))
                bytes.copyTo(out.slice(outPos))
                outPos = checkedAdd(outPos, bytes.size)
            }
            if (line.isEmpty() && !lineReader.isEmpty()) advanceLine()
            val inBlocks = line.len() / 4
            val outBlocks = checkedSub(out.size, outPos) / 3
            val blocks = minOf(inBlocks, outBlocks)
            val inAligned = line.take(blocks * 4)
            if (!inAligned.isEmpty()) {
                val outBuf = out.slice(outPos, checkedAdd(outPos, blocks * 3))
                outPos = checkedAdd(outPos, performDecode(inAligned, outBuf).size)
            }
            if (outPos < out.size) {
                if (isFinished()) fail(Error.InvalidLength)
                fillBlockBuffer()
            }
        }
        remainingLength = checkedSub(remainingLength, out.size)
        return out
    }

    /** Append all remaining decoded bytes to the supplied byte list. */
    fun decodeToEnd(buf: MutableList<Byte>): DecodeVectorResult = result(::DecodeVectorResult) { decodeToEndInto(buf) }

    private fun decodeToEndInto(buf: MutableList<Byte>): ByteArray {
        val startLen = buf.size
        val count = remainingLen()
        checkedAdd(startLen, count)
        val output = ByteArray(count)
        repeat(count) { buf.add(0) }
        try {
            decodeInto(output.asSlice())
        } finally {
            for (i in output.indices) buf[startLen + i] = output[i]
        }
        return output
    }

    fun remainingLen(): Int = remainingLength

    fun isFinished(): Boolean = line.isEmpty() && lineReader.isEmpty() && blockBuffer.isEmpty()

    fun clone(): Decoder<E> = Decoder(line.clone(), lineReader.clone(), remainingLength, blockBuffer.clone(), encoding)

    private fun fillBlockBuffer() {
        val buf = ByteArray(3).asSlice()
        val decoded =
            if (line.len() < 4 && !lineReader.isEmpty()) {
                val tmp = ByteArray(4).asSlice()
                val lineEnd = line.take(4)
                lineEnd.copyTo(tmp)
                advanceLine()
                val lineBegin = line.take(checkedSub(4, lineEnd.size))
                lineBegin.copyTo(tmp.slice(lineEnd.size))
                performDecode(tmp.slice(0, checkedAdd(lineBegin.size, lineEnd.size)), buf)
            } else {
                performDecode(line.take(4), buf)
            }
        blockBuffer.fill(decoded)
    }

    private fun advanceLine() {
        check(line.isEmpty())
        line = lineReader.next() ?: fail(Error.InvalidLength)
    }

    private fun performDecode(src: ByteSlice, dst: ByteSlice): ByteSlice =
        if (isFinished()) encoding.decodeSlice(src, dst) else encoding.unpadded.decodeSlice(src, dst)

    /** Read at most the output capacity, returning zero after the input ends. */
    fun read(buf: ByteArray): ReadResult =
        result(::ReadResult) {
            if (isFinished()) {
                0
            } else {
                val slice = buf.asSlice().slice(0, minOf(buf.size, remainingLen()))
                decodeInto(slice).size
            }
        }

    fun readToEnd(buf: MutableList<Byte>): ReadResult =
        result(::ReadResult) {
            if (isFinished()) 0 else decodeToEndInto(buf).size
        }

    fun readExact(buf: ByteArray): OperationResult =
        result(::OperationResult) {
            decodeInto(buf.asSlice())
            return@result
        }
}

private class BlockBuffer(
    private val decoded: ByteArray = ByteArray(3),
    private var length: Int = 0,
    private var position: Int = 0,
) {
    fun fill(input: ByteSlice) {
        check(isEmpty())
        if (input.size > 3) fail(Error.InvalidLength)
        position = 0
        length = input.size
        input.copyTo(decoded.asSlice())
    }

    fun take(nbytes: Int): ByteSlice {
        val startPos = position
        val count = minOf(nbytes, checkedSub(length, startPos))
        position = checkedAdd(position, count)
        return decoded.asSlice().slice(startPos, position)
    }

    fun isEmpty(): Boolean = position == length

    fun clone(): BlockBuffer = BlockBuffer(decoded.copyOf(), length, position)
}

/** A single line of wrapped input. */
class Line internal constructor(
    internal var remaining: ByteSlice = ByteArray(0).asSlice(),
) {
    internal fun take(nbytes: Int): ByteSlice {
        val count = minOf(nbytes, remaining.size)
        val bytes = remaining.slice(0, count)
        remaining = remaining.slice(count)
        return bytes
    }

    internal fun sliceTail(nbytes: Int): ByteSlice = remaining.slice(checkedSub(len(), nbytes))

    internal fun len(): Int = remaining.size

    internal fun isEmpty(): Boolean = len() == 0

    internal fun trimEnd(): Line {
        val length = len()
        val removed =
            when {
                length >= 2 && remaining[length - 2] == CHAR_CR && remaining[length - 1] == CHAR_LF -> 2
                length >= 1 && (remaining[length - 1] == CHAR_CR || remaining[length - 1] == CHAR_LF) -> 1
                else -> 0
            }
        return Line(remaining.slice(0, length - removed))
    }

    internal fun clone(): Line = Line(remaining)
}

private class LineReader(
    private var remaining: ByteSlice,
    private val lineWidth: Int?,
) {
    companion object {
        fun newUnwrapped(bytes: ByteSlice): LineReader {
            if (bytes.isEmpty()) fail(Error.InvalidLength)
            return LineReader(bytes, null)
        }

        fun newWrapped(bytes: ByteSlice, width: Int): LineReader {
            if (width < MIN_LINE_WIDTH || bytes.isEmpty()) fail(Error.InvalidLength)
            return LineReader(bytes, width)
        }
    }

    fun isEmpty(): Boolean = remaining.isEmpty()

    fun decodedLen(encoding: Encoding): Int {
        val buffer = ByteArray(4).asSlice()
        val lines = clone()
        var line = lines.next() ?: return 0
        var base64Len = 0
        while (true) {
            base64Len = checkedAdd(base64Len, line.len())
            val next = lines.next()
            if (next != null) {
                line.sliceTail(4).copyTo(buffer)
                line = next
            } else {
                val lastBlockSize = if (base64Len % 4 == 0) 4 else base64Len % 4
                val length = decodedLen(checkedSub(base64Len, lastBlockSize))
                val out = ByteArray(3).asSlice()
                val lastBlockLen =
                    if (line.len() < lastBlockSize) {
                        val bufferedPartLen = checkedSub(lastBlockSize, line.len())
                        val offset = checkedSub(4, bufferedPartLen)
                        for (i in 0 until bufferedPartLen) buffer[i] = buffer[checkedAdd(offset, i)]
                        line.remaining.copyTo(buffer.slice(bufferedPartLen))
                        encoding.decodeSlice(buffer.slice(0, checkedAdd(bufferedPartLen, line.len())), out).size
                    } else {
                        encoding.decodeSlice(line.sliceTail(lastBlockSize), out).size
                    }
                return checkedAdd(length, lastBlockLen)
            }
        }
    }

    fun next(): Line? {
        val width = lineWidth
        if (width != null) {
            if (remaining.size <= width) {
                if (remaining.isEmpty()) return null
                val line = Line(remaining).trimEnd()
                remaining = remaining.slice(remaining.size)
                return line
            }
            val rest = remaining.slice(width)
            val newlineSize =
                when {
                    rest.size >= 2 && rest[0] == CHAR_CR && rest[1] == CHAR_LF -> 2
                    rest[0] == CHAR_CR || rest[0] == CHAR_LF -> 1
                    else -> fail(Error.InvalidEncoding)
                }
            val line = Line(remaining.slice(0, width))
            remaining = rest.slice(newlineSize)
            return line
        }
        if (remaining.isEmpty()) return null
        val line = Line(remaining).trimEnd()
        remaining = remaining.slice(remaining.size)
        return if (line.isEmpty()) null else line
    }

    fun clone(): LineReader = LineReader(remaining, lineWidth)
}
