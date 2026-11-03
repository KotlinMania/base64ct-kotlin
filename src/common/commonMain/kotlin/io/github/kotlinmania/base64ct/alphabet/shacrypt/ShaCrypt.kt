// port-lint: source src/alphabet/shacrypt.rs
package io.github.kotlinmania.base64ct.alphabet.shacrypt

import io.github.kotlinmania.base64ct.ByteSlice
import io.github.kotlinmania.base64ct.alphabet.DecodeStep
import io.github.kotlinmania.base64ct.alphabet.EncodeStep
import io.github.kotlinmania.base64ct.encoding.Encoding

/** Standard little endian Base64 used by crypt password hash schemes. */
data object Base64ShaCrypt : Encoding {
    override val base: Int = '.'.code
    override val decoder: List<DecodeStep> =
        listOf(
            DecodeStep.Range('.'.code, '9'.code, -45),
            DecodeStep.Range('A'.code, 'Z'.code, -52),
            DecodeStep.Range('a'.code, 'z'.code, -58),
        )
    override val encoder: List<EncodeStep> =
        listOf(
            EncodeStep.Apply('9'.code, 7),
            EncodeStep.Apply('Z'.code, 6),
        )
    override val padded: Boolean = false
    override val unpadded: Encoding get() = this

    override fun decode3bytes(src: ByteSlice, dst: ByteSlice): Int {
        require(src.size == 4 && dst.size >= 3)
        val c0 = decode6bits(src[0])
        val c1 = decode6bits(src[1])
        val c2 = decode6bits(src[2])
        val c3 = decode6bits(src[3])
        dst[0] = (c0 or ((c1 and 3) shl 6)).toByte()
        dst[1] = ((c1 shr 2) or ((c2 and 15) shl 4)).toByte()
        dst[2] = ((c2 shr 4) or (c3 shl 2)).toByte()
        return ((c0 or c1 or c2 or c3) shr 8) and 1
    }

    override fun encode3bytes(src: ByteSlice, dst: ByteSlice) {
        require(src.size == 3 && dst.size >= 4)
        val b0 = src[0].toInt() and 255
        val b1 = src[1].toInt() and 255
        val b2 = src[2].toInt() and 255
        dst[0] = encode6bits(b0 and 63)
        dst[1] = encode6bits(((b1 shl 2) or (b0 shr 6)) and 63)
        dst[2] = encode6bits(((b2 shl 4) or (b1 shr 4)) and 63)
        dst[3] = encode6bits(b2 shr 2)
    }
}
