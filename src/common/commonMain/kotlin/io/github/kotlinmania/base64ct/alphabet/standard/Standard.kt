// port-lint: source src/alphabet/standard.rs
package io.github.kotlinmania.base64ct.alphabet.standard

import io.github.kotlinmania.base64ct.alphabet.DecodeStep
import io.github.kotlinmania.base64ct.alphabet.EncodeStep
import io.github.kotlinmania.base64ct.encoding.Encoding

/** Standard Base64 encoding with equals padding. */
data object Base64 : Encoding {
    override val base: Int = 'A'.code
    override val decoder: List<DecodeStep> get() = standardDecoder
    override val encoder: List<EncodeStep> get() = standardEncoder
    override val padded: Boolean = true
    override val unpadded: Encoding get() = Base64Unpadded
}

/** Standard Base64 encoding without padding. */
data object Base64Unpadded : Encoding {
    override val base: Int = 'A'.code
    override val decoder: List<DecodeStep> get() = standardDecoder
    override val encoder: List<EncodeStep> get() = standardEncoder
    override val padded: Boolean = false
    override val unpadded: Encoding get() = this
}

private val standardDecoder =
    listOf(
        DecodeStep.Range('A'.code, 'Z'.code, -64),
        DecodeStep.Range('a'.code, 'z'.code, -70),
        DecodeStep.Range('0'.code, '9'.code, 5),
        DecodeStep.Eq('+'.code, 63),
        DecodeStep.Eq('/'.code, 64),
    )

private val standardEncoder =
    listOf(
        EncodeStep.Diff(25, 6),
        EncodeStep.Diff(51, -75),
        EncodeStep.Diff(61, -('+'.code - 0x1c)),
        EncodeStep.Diff(62, '/'.code - '+'.code - 1),
    )
