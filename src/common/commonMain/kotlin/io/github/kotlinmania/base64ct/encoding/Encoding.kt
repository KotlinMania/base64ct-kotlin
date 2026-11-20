// port-lint: source src/encoding.rs
package io.github.kotlinmania.base64ct.encoding

import io.github.kotlinmania.base64ct.ByteSlice
import io.github.kotlinmania.base64ct.DecodeResult
import io.github.kotlinmania.base64ct.DecodeVectorResult
import io.github.kotlinmania.base64ct.EncodeResult
import io.github.kotlinmania.base64ct.alphabet.Alphabet
import io.github.kotlinmania.base64ct.asSlice
import io.github.kotlinmania.base64ct.errors.Error
import io.github.kotlinmania.base64ct.fail
import io.github.kotlinmania.base64ct.result

private const val PAD = 61

/** Operations shared by the supported Base64 alphabets. */
interface Encoding : Alphabet {
    fun decode(src: ByteArray, dst: ByteArray): DecodeResult =
        result(::DecodeResult) {
            decodeSlice(src.asSlice(), dst.asSlice())
        }

    fun decode(src: String, dst: ByteArray): DecodeResult = decode(src.encodeToByteArray(), dst)

    /** Decode in place, without validating canonical trailing padding bits. */
    fun decodeInPlace(buf: ByteArray): DecodeResult =
        result(::DecodeResult) {
            var input = buf.asSlice()
            var err = 0
            if (padded) {
                val padding = decodePadding(input)
                input = input.slice(0, padding.unpaddedLen)
                err = padding.error
            }
            val dlen = decodedLen(input.size)
            val fullChunks = input.size / 4
            for (chunk in 0 until fullChunks) {
                val tmpOut = ByteArray(3).asSlice()
                err = err or decode3bytes(input.slice(4 * chunk, 4 * chunk + 4), tmpOut)
                tmpOut.copyTo(input.slice(3 * chunk))
            }
            val srcRemPos = 4 * fullChunks
            val srcRemLen = input.size - srcRemPos
            val dstRemPos = 3 * fullChunks
            val dstRemLen = dlen - dstRemPos
            err = err or if (srcRemLen == 0 || srcRemLen >= 2) 0 else 1
            val tmpIn = ByteArray(4) { 'A'.code.toByte() }.asSlice()
            input.slice(srcRemPos).copyTo(tmpIn)
            val tmpOut = ByteArray(3).asSlice()
            err = err or decode3bytes(tmpIn, tmpOut)
            if (err != 0) fail(Error.InvalidEncoding)
            tmpOut.slice(0, dstRemLen).copyTo(input.slice(dstRemPos))
            input.slice(0, dlen)
        }

    fun decodeVec(input: String): DecodeVectorResult =
        result(::DecodeVectorResult) {
            val bytes = input.encodeToByteArray()
            val output = ByteArray(decodedLen(bytes.size))
            decodeSlice(bytes.asSlice(), output.asSlice()).toByteArray()
        }

    fun encode(src: ByteArray, dst: ByteArray): EncodeResult =
        result(::EncodeResult) {
            encodeSlice(src.asSlice(), dst.asSlice()).text()
        }

    fun encodeString(input: ByteArray): String {
        val elen = requireNotNull(encodedLenInner(input.size, padded)) { "input is too big" }
        val dst = ByteArray(elen)
        return encodeSlice(input.asSlice(), dst.asSlice()).text()
    }

    fun encodedLen(bytes: ByteArray): Int = encodedLenInner(bytes.size, padded) ?: 0
}

internal fun Encoding.decodeSlice(src: ByteSlice, destination: ByteSlice): ByteSlice {
    val padding = if (padded) decodePadding(src) else Padding(src.size, 0)
    val unpadded = src.slice(0, padding.unpaddedLen)
    var err = padding.error
    val dlen = decodedLen(unpadded.size)
    if (dlen > destination.size) fail(Error.InvalidLength)
    val dst = destination.slice(0, dlen)
    val fullChunks = unpadded.size / 4
    for (chunk in 0 until fullChunks) {
        err = err or decode3bytes(unpadded.slice(chunk * 4, chunk * 4 + 4), dst.slice(chunk * 3))
    }
    val srcRem = unpadded.slice(fullChunks * 4)
    val dstRem = dst.slice(fullChunks * 3)
    err = err or if (srcRem.isEmpty() || srcRem.size >= 2) 0 else 1
    val tmpOut = ByteArray(3).asSlice()
    val tmpIn = ByteArray(4) { 'A'.code.toByte() }.asSlice()
    srcRem.copyTo(tmpIn)
    err = err or decode3bytes(tmpIn, tmpOut)
    tmpOut.slice(0, dstRem.size).copyTo(dstRem)
    if (err != 0) fail(Error.InvalidEncoding)
    validateLastBlock(src, dst)
    return dst
}

internal fun Encoding.encodeSlice(src: ByteSlice, destination: ByteSlice): ByteSlice {
    val elen = encodedLenInner(src.size, padded) ?: fail(Error.InvalidLength)
    if (elen > destination.size) fail(Error.InvalidLength)
    val dst = destination.slice(0, elen)
    val fullChunks = src.size / 3
    for (chunk in 0 until fullChunks) {
        encode3bytes(src.slice(chunk * 3, chunk * 3 + 3), dst.slice(chunk * 4))
    }
    val srcRem = src.slice(fullChunks * 3)
    val dstRem = dst.slice(fullChunks * 4)
    if (padded) {
        if (!dstRem.isEmpty()) {
            val tmp = ByteArray(3).asSlice()
            srcRem.copyTo(tmp)
            encode3bytes(tmp, dstRem)
            val mask = (if (srcRem.size == 1) 1 else 0) - 1
            dstRem[2] = ((dstRem[2].toInt() and mask) or (PAD and mask.inv())).toByte()
            dstRem[3] = PAD.toByte()
        }
    } else {
        val tmpIn = ByteArray(3).asSlice()
        val tmpOut = ByteArray(4).asSlice()
        srcRem.copyTo(tmpIn)
        encode3bytes(tmpIn, tmpOut)
        tmpOut.slice(0, dstRem.size).copyTo(dstRem)
    }
    return dst
}

internal class Padding(
    val unpaddedLen: Int,
    val error: Int,
)

internal fun decodePadding(input: ByteSlice): Padding {
    if (input.size % 4 != 0) fail(Error.InvalidEncoding)
    val paddingLen = if (input.size >= 2) isPadCt(input[input.size - 2]) + isPadCt(input[input.size - 1]) else 0
    val unpaddedLen = input.size - paddingLen
    val err =
        when (paddingLen) {
            1 -> isPadCt(input[input.size - 1]) xor 1
            2 -> (isPadCt(input[input.size - 2]) and isPadCt(input[input.size - 1])) xor 1
            0 -> 0
            else -> fail(Error.InvalidEncoding)
        }
    return Padding(unpaddedLen, err)
}

private fun Encoding.validateLastBlock(encoded: ByteSlice, decoded: ByteSlice) {
    if (encoded.isEmpty() && decoded.isEmpty()) return

    fun lastBlockStart(bytes: ByteSlice, blockSize: Int): Int = ((bytes.size - 1).coerceAtLeast(0) / blockSize) * blockSize
    val encBlock = encoded.slice(lastBlockStart(encoded, 4))
    val decBlock = decoded.slice(lastBlockStart(decoded, 3))
    val block = encodeSlice(decBlock, ByteArray(4).asSlice())
    var difference = 0
    for (i in 0 until minOf(block.size, encBlock.size)) {
        difference = difference or (block[i].toInt() xor encBlock[i].toInt())
    }
    if (difference != 0) fail(Error.InvalidEncoding)
}

internal fun decodedLen(inputLen: Int): Int {
    val k = inputLen / 4
    val remainder = inputLen - 4 * k
    return 3 * k + (3 * remainder) / 4
}

private fun isPadCt(input: Byte): Int {
    val value = input.toInt() and 255
    return (((PAD - 1 - value) and (value - (PAD + 1))) shr 8) and 1
}

internal fun encodedLenInner(n: Int, padded: Boolean): Int? {
    if (n < 0) return null
    val len = ((n.toLong() + 2) / 3) * 4
    if (len > Int.MAX_VALUE) return null
    val rem = n % 3
    return len.toInt() - if (padded || rem == 0) 0 else 3 - rem
}
