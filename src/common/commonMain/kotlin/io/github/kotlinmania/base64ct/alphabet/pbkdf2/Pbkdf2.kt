// port-lint: source src/alphabet/pbkdf2.rs
package io.github.kotlinmania.base64ct.alphabet.pbkdf2

import io.github.kotlinmania.base64ct.alphabet.DecodeStep
import io.github.kotlinmania.base64ct.alphabet.EncodeStep
import io.github.kotlinmania.base64ct.encoding.Encoding

/** Unpadded standard Base64 with a dot instead of a plus. */
data object Base64Pbkdf2 : Encoding {
    override val base: Int = 'A'.code
    override val decoder: List<DecodeStep> get() = pbkdf2Decoder
    override val encoder: List<EncodeStep> get() = pbkdf2Encoder
    override val padded: Boolean = false
    override val unpadded: Encoding get() = this
}

private val pbkdf2Decoder =
    listOf(
        DecodeStep.Range('A'.code, 'Z'.code, -64),
        DecodeStep.Range('a'.code, 'z'.code, -70),
        DecodeStep.Range('0'.code, '9'.code, 5),
        DecodeStep.Eq('.'.code, 63),
        DecodeStep.Eq('/'.code, 64),
    )

private val pbkdf2Encoder =
    listOf(
        EncodeStep.Diff(25, 6),
        EncodeStep.Diff(51, -75),
        EncodeStep.Diff(61, -('.'.code - 0x22)),
        EncodeStep.Diff(62, '/'.code - '.'.code - 1),
    )
