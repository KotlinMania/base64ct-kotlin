// port-lint: source src/alphabet/url.rs
package io.github.kotlinmania.base64ct.alphabet.url

import io.github.kotlinmania.base64ct.alphabet.DecodeStep
import io.github.kotlinmania.base64ct.alphabet.EncodeStep
import io.github.kotlinmania.base64ct.encoding.Encoding

/** URL-safe Base64 encoding with equals padding. */
data object Base64Url : Encoding {
    override val base: Int = 'A'.code
    override val decoder: List<DecodeStep> get() = urlDecoder
    override val encoder: List<EncodeStep> get() = urlEncoder
    override val padded: Boolean = true
    override val unpadded: Encoding get() = Base64UrlUnpadded
}

/** URL-safe Base64 encoding without padding. */
data object Base64UrlUnpadded : Encoding {
    override val base: Int = 'A'.code
    override val decoder: List<DecodeStep> get() = urlDecoder
    override val encoder: List<EncodeStep> get() = urlEncoder
    override val padded: Boolean = false
    override val unpadded: Encoding get() = this
}

private val urlDecoder =
    listOf(
        DecodeStep.Range('A'.code, 'Z'.code, -64),
        DecodeStep.Range('a'.code, 'z'.code, -70),
        DecodeStep.Range('0'.code, '9'.code, 5),
        DecodeStep.Eq('-'.code, 63),
        DecodeStep.Eq('_'.code, 64),
    )

private val urlEncoder =
    listOf(
        EncodeStep.Diff(25, 6),
        EncodeStep.Diff(51, -75),
        EncodeStep.Diff(61, -('-'.code - 0x20)),
        EncodeStep.Diff(62, '_'.code - '-'.code - 1),
    )
