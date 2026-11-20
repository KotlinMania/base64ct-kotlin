// port-lint: tests tests/bcrypt.rs
package io.github.kotlinmania.base64ct

import io.github.kotlinmania.base64ct.alphabet.bcrypt.Base64Bcrypt
import io.github.kotlinmania.base64ct.errors.Error
import kotlin.test.Test
import kotlin.test.assertEquals

class BcryptTest {
    private val testVectors =
        vectors(
            "",
            "..",
            "Igmo",
            ".OGB/.",
            "pY0rpYy",
            "98989898",
            "9999996",
            "OKC9tOTKagohutGPa6/n4ij7LQjpxAPj7tlOOOf5z4i",
            "./ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwx89..",
        )

    @Test fun encodeTestVectors() = encodeTestVectors(Base64Bcrypt, testVectors)

    @Test fun decodeTestVectors() = decodeTestVectors(Base64Bcrypt, testVectors)

    @Test fun encodeAndDecodeVariousLengths() = encodeAndDecodeVariousLengths(Base64Bcrypt)

    @Test fun rejectTrailingWhitespace() {
        assertEquals(Error.InvalidEncoding, Base64Bcrypt.decode("OKC9tOTKagohutGPa6/n4ij7LQjpxAPj7tlOOOf5z4i\n", ByteArray(1024)).error)
    }

    @Test fun unpaddedRejectTrailingEquals() {
        assertEquals(Error.InvalidEncoding, Base64Bcrypt.decode("OKC9tOTKagohutGPa6/n4ij7LQjpxAPj7tlOOOf5z4i=", ByteArray(1024)).error)
    }
}
