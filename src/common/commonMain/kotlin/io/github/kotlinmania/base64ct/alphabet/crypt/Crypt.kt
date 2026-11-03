// port-lint: source src/alphabet/crypt.rs
package io.github.kotlinmania.base64ct.alphabet.crypt

import io.github.kotlinmania.base64ct.alphabet.DecodeStep
import io.github.kotlinmania.base64ct.alphabet.EncodeStep
import io.github.kotlinmania.base64ct.encoding.Encoding

/**
 * Deprecated non-standard big endian crypt encoding.
 * Use Base64ShaCrypt for modern crypt algorithms.
 */
data object Base64Crypt : Encoding {
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
}
