// port-lint: tests tests/crypt.rs
package io.github.kotlinmania.base64ct

import io.github.kotlinmania.base64ct.alphabet.crypt.Base64Crypt
import io.github.kotlinmania.base64ct.errors.Error
import kotlin.test.Test
import kotlin.test.assertEquals

class CryptTest {
    private val testVectors =
        vectors(
            "",
            "..",
            "8Wce",
            ".E61/.",
            "fOqhfOo",
            "zyzyzyzy",
            "zzzzzzw",
            "EA2zjEJAQWeXkj6FQw/duYZxBGZfn0FZxjbEEEVvpuY",
            "./0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnyz..",
        )

    @Test fun encodeTestVectors() = encodeTestVectors(Base64Crypt, testVectors)

    @Test fun decodeTestVectors() = decodeTestVectors(Base64Crypt, testVectors)

    @Test fun encodeAndDecodeVariousLengths() = encodeAndDecodeVariousLengths(Base64Crypt)

    @Test fun rejectTrailingWhitespace() {
        assertEquals(Error.InvalidEncoding, Base64Crypt.decode("OKC9tOTKagohutGPa6/n4ij7LQjpxAPj7tlOOOf5z4i\n", ByteArray(1024)).error)
    }

    @Test fun unpaddedRejectTrailingEquals() {
        assertEquals(Error.InvalidEncoding, Base64Crypt.decode("OKC9tOTKagohutGPa6/n4ij7LQjpxAPj7tlOOOf5z4i=", ByteArray(1024)).error)
    }
}
