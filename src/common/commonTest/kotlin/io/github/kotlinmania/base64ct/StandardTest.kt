// port-lint: tests tests/standard.rs
package io.github.kotlinmania.base64ct

import io.github.kotlinmania.base64ct.alphabet.standard.Base64
import io.github.kotlinmania.base64ct.alphabet.standard.Base64Unpadded
import io.github.kotlinmania.base64ct.errors.Error
import kotlin.test.Test
import kotlin.test.assertEquals

class StandardTest {
    private val padded =
        vectors(
            "",
            "AA==",
            "Kioq",
            "AQIDBA==",
            "ra2tra0=",
            "/+/+/+/+",
            "//////8=",
            "QME/vQVMciqjwvIRc8Bp6kl9NSlrzCRl9vnQQQh716k=",
            "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz+/AA",
        )
    private val unpadded =
        vectors(
            "",
            "AA",
            "Kioq",
            "AQIDBA",
            "ra2tra0",
            "/+/+/+/+",
            "//////8",
            "QME/vQVMciqjwvIRc8Bp6kl9NSlrzCRl9vnQQQh716k",
            "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz+/AA",
        )

    @Test fun encodePaddedTestVectors() = encodeTestVectors(Base64, padded)

    @Test fun decodePaddedTestVectors() = decodeTestVectors(Base64, padded)

    @Test fun paddedVariousLengths() = encodeAndDecodeVariousLengths(Base64)

    @Test fun rejectPaddedTrailingWhitespace() {
        assertEquals(Error.InvalidEncoding, Base64.decode("QME/vQVMciqjwvIRc8Bp6kl9NSlrzCRl9vnQQQh716k\n", ByteArray(1024)).error)
    }

    @Test fun rejectInvalidPadding() {
        assertEquals(Error.InvalidEncoding, Base64.decode("AA/=", ByteArray(1024)).error)
    }

    @Test fun encodeUnpaddedTestVectors() = encodeTestVectors(Base64Unpadded, unpadded)

    @Test fun decodeUnpaddedTestVectors() = decodeTestVectors(Base64Unpadded, unpadded)

    @Test fun unpaddedVariousLengths() = encodeAndDecodeVariousLengths(Base64Unpadded)

    @Test fun rejectUnpaddedTrailingWhitespace() {
        assertEquals(Error.InvalidEncoding, Base64Unpadded.decode("EA2zjEJAQWeXkj6FQw/duYZxBGZfn0FZxjbEEEVvpuY\n", ByteArray(1024)).error)
    }

    @Test fun unpaddedRejectTrailingEquals() {
        assertEquals(Error.InvalidEncoding, Base64Unpadded.decode("EA2zjEJAQWeXkj6FQw/duYZxBGZfn0FZxjbEEEVvpuY=", ByteArray(1024)).error)
    }

    @Test fun rejectNonCanonicalEncoding() {
        assertEquals(Error.InvalidEncoding, Base64Unpadded.decode("Mi", ByteArray(8)).error)
    }
}
