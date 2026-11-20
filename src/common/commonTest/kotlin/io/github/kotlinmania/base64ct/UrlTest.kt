// port-lint: tests tests/url.rs
package io.github.kotlinmania.base64ct

import io.github.kotlinmania.base64ct.alphabet.url.Base64Url
import io.github.kotlinmania.base64ct.alphabet.url.Base64UrlUnpadded
import io.github.kotlinmania.base64ct.errors.Error
import kotlin.test.Test
import kotlin.test.assertEquals

class UrlTest {
    private val padded =
        vectors(
            "",
            "AA==",
            "Kioq",
            "AQIDBA==",
            "ra2tra0=",
            "_-_-_-_-",
            "______8=",
            "QME_vQVMciqjwvIRc8Bp6kl9NSlrzCRl9vnQQQh716k=",
            "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz-_AA",
        )
    private val unpadded =
        vectors(
            "",
            "AA",
            "Kioq",
            "AQIDBA",
            "ra2tra0",
            "_-_-_-_-",
            "______8",
            "QME_vQVMciqjwvIRc8Bp6kl9NSlrzCRl9vnQQQh716k",
            "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz-_AA",
        )

    @Test fun encodePaddedTestVectors() = encodeTestVectors(Base64Url, padded)

    @Test fun decodePaddedTestVectors() = decodeTestVectors(Base64Url, padded)

    @Test fun paddedVariousLengths() = encodeAndDecodeVariousLengths(Base64Url)

    @Test fun rejectPaddedTrailingWhitespace() {
        assertEquals(Error.InvalidEncoding, Base64Url.decode("QME/vQVMciqjwvIRc8Bp6kl9NSlrzCRl9vnQQQh716k\n", ByteArray(1024)).error)
    }

    @Test fun encodeUnpaddedTestVectors() = encodeTestVectors(Base64UrlUnpadded, unpadded)

    @Test fun decodeUnpaddedTestVectors() = decodeTestVectors(Base64UrlUnpadded, unpadded)

    @Test fun unpaddedVariousLengths() = encodeAndDecodeVariousLengths(Base64UrlUnpadded)

    @Test fun rejectUnpaddedTrailingWhitespace() {
        assertEquals(Error.InvalidEncoding, Base64UrlUnpadded.decode("QME/vQVMciqjwvIRc8Bp6kl9NSlrzCRl9vnQQQh716k\n", ByteArray(1024)).error)
    }

    @Test fun unpaddedRejectTrailingEquals() {
        assertEquals(Error.InvalidEncoding, Base64UrlUnpadded.decode("QME_vQVMciqjwvIRc8Bp6kl9NSlrzCRl9vnQQQh716k=", ByteArray(1024)).error)
    }
}
