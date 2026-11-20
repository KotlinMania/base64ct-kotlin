// port-lint: source src/alphabet/bcrypt.rs
package io.github.kotlinmania.base64ct.alphabet.bcrypt

import io.github.kotlinmania.base64ct.alphabet.DecodeStep
import io.github.kotlinmania.base64ct.alphabet.EncodeStep
import io.github.kotlinmania.base64ct.encoding.Encoding

/** bcrypt Base64 encoding. */
data object Base64Bcrypt : Encoding {
    override val base: Int = '.'.code
    override val decoder: List<DecodeStep> =
        listOf(
            DecodeStep.Range('.'.code, '/'.code, -45),
            DecodeStep.Range('A'.code, 'Z'.code, -62),
            DecodeStep.Range('a'.code, 'z'.code, -68),
            DecodeStep.Range('0'.code, '9'.code, 7),
        )
    override val encoder: List<EncodeStep> =
        listOf(
            EncodeStep.Apply('/'.code, 17),
            EncodeStep.Apply('Z'.code, 6),
            EncodeStep.Apply('z'.code, -75),
        )
    override val padded: Boolean = false
    override val unpadded: Encoding get() = this
}
