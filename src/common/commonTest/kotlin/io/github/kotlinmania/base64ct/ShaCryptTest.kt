// port-lint: tests tests/shacrypt.rs
package io.github.kotlinmania.base64ct

import io.github.kotlinmania.base64ct.alphabet.shacrypt.Base64ShaCrypt
import io.github.kotlinmania.base64ct.errors.Error
import kotlin.test.Test
import kotlin.test.assertEquals

class ShaCryptTest {
    private val testVectors =
        listOf(
            TestVector(hex(""), ""),
            TestVector(hex("55"), "J/"),
            TestVector(hex("55aa"), "Jd8"),
            TestVector(hex("55aa55"), "JdOJ"),
            TestVector(hex("55aa55aa"), "JdOJe0"),
            TestVector(hex("55aa55aa55"), "JdOJeK3"),
            TestVector(hex("55aa55aa55aa"), "JdOJeKZe"),
            TestVector(hex("55aa55af"), "JdOJj0"),
            TestVector(hex("55aa55aa5f"), "JdOJey3"),
            TestVector(hex("00"), ".."),
            TestVector(hex("2a2a2a"), "ecW8"),
            TestVector(hex("01020304"), "/6k.2."),
            TestVector(hex("adadadadad"), "hqOfhq8"),
            TestVector(hex("ffeffeffeffe"), "zzizzziz"),
            TestVector(hex("ffffffffff"), "zzzzzzD"),
            TestVector(hex("40c13fbd054c722aa3c2f21173c069ea497d35296bcc2465f6f9d041087bd7a9"), ".3wDxK.Hmdmc09T2n/QOebITpYmOAHGNqbDo/VkSLb8"),
            TestVector(hex("40200c44611c48a22c4ce33c50244d54655d58a66d5ce77d60288e64699e68aaae6cebbe702ccffe0f00"), "./0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnyz.."),
        )

    @Test fun encodeTestVectors() = encodeTestVectors(Base64ShaCrypt, testVectors)

    @Test fun decodeTestVectors() = decodeTestVectors(Base64ShaCrypt, testVectors)

    @Test fun encodeAndDecodeVariousLengths() = encodeAndDecodeVariousLengths(Base64ShaCrypt)

    @Test fun rejectTrailingWhitespace() {
        assertEquals(Error.InvalidEncoding, Base64ShaCrypt.decode("OKC9tOTKagohutGPa6/n4ij7LQjpxAPj7tlOOOf5z4i\n", ByteArray(1024)).error)
    }

    @Test fun unpaddedRejectTrailingEquals() {
        assertEquals(Error.InvalidEncoding, Base64ShaCrypt.decode("OKC9tOTKagohutGPa6/n4ij7LQjpxAPj7tlOOOf5z4i=", ByteArray(1024)).error)
    }
}
