// port-lint: source src/alphabet.rs
package io.github.kotlinmania.base64ct.alphabet

import io.github.kotlinmania.base64ct.ByteSlice
import io.github.kotlinmania.base64ct.encoding.Encoding

/** Core arithmetic encoder/decoder operations for a Base64 alphabet. */
interface Alphabet {
    val base: Int
    val decoder: List<DecodeStep>
    val encoder: List<EncodeStep>
    val padded: Boolean
    val unpadded: Encoding

    fun decode3bytes(src: ByteSlice, dst: ByteSlice): Int {
        require(src.size == 4 && dst.size >= 3)
        val c0 = decode6bits(src[0])
        val c1 = decode6bits(src[1])
        val c2 = decode6bits(src[2])
        val c3 = decode6bits(src[3])
        dst[0] = ((c0 shl 2) or (c1 shr 4)).toByte()
        dst[1] = ((c1 shl 4) or (c2 shr 2)).toByte()
        dst[2] = ((c2 shl 6) or c3).toByte()
        return ((c0 or c1 or c2 or c3) shr 8) and 1
    }

    fun decode6bits(src: Byte): Int {
        val value = src.toInt() and 255
        var ret = -1
        for (step in decoder) {
            ret +=
                when (step) {
                    is DecodeStep.Range -> {
                        val start = step.first - 1
                        val end = step.last + 1
                        (((start - value) and (value - end)) shr 8) and (value + step.offset)
                    }

                    is DecodeStep.Eq -> {
                        val start = step.value - 1
                        val end = step.value + 1
                        (((start - value) and (value - end)) shr 8) and step.offset
                    }
                }
        }
        return ret
    }

    fun encode3bytes(src: ByteSlice, dst: ByteSlice) {
        require(src.size == 3 && dst.size >= 4)
        val b0 = src[0].toInt() and 255
        val b1 = src[1].toInt() and 255
        val b2 = src[2].toInt() and 255
        dst[0] = encode6bits(b0 shr 2)
        dst[1] = encode6bits(((b0 shl 4) or (b1 shr 4)) and 63)
        dst[2] = encode6bits(((b1 shl 2) or (b2 shr 6)) and 63)
        dst[3] = encode6bits(b2 and 63)
    }

    fun encode6bits(src: Int): Byte {
        var diff = src + base
        for (step in encoder) {
            diff +=
                when (step) {
                    is EncodeStep.Apply -> ((step.threshold - diff) shr 8) and step.offset
                    is EncodeStep.Diff -> ((step.threshold - src) shr 8) and step.offset
                }
        }
        return diff.toByte()
    }
}

/** A constant-time decoder step. */
sealed class DecodeStep {
    class Range(
        val first: Int,
        val last: Int,
        val offset: Int,
    ) : DecodeStep()

    class Eq(
        val value: Int,
        val offset: Int,
    ) : DecodeStep()
}

/** A constant-time encoder step. */
sealed class EncodeStep {
    class Apply(
        val threshold: Int,
        val offset: Int,
    ) : EncodeStep()

    class Diff(
        val threshold: Int,
        val offset: Int,
    ) : EncodeStep()
}
