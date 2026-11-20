// port-lint: tests tests/pbkdf2.rs
package io.github.kotlinmania.base64ct

import io.github.kotlinmania.base64ct.alphabet.pbkdf2.Base64Pbkdf2
import io.github.kotlinmania.base64ct.errors.Error
import kotlin.test.Test
import kotlin.test.assertEquals

class Pbkdf2Test {
    private val testVectors =
        vectors(
            "",
            "AA",
            "Kioq",
            "AQIDBA",
            "ra2tra0",
            "/./././.",
            "//////8",
            "QME/vQVMciqjwvIRc8Bp6kl9NSlrzCRl9vnQQQh716k",
            "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz./AA",
        )

    @Test fun encodeTestVectors() = encodeTestVectors(Base64Pbkdf2, testVectors)

    @Test fun decodeTestVectors() = decodeTestVectors(Base64Pbkdf2, testVectors)

    @Test fun encodeAndDecodeVariousLengths() = encodeAndDecodeVariousLengths(Base64Pbkdf2)

    @Test fun rejectTrailingWhitespace() {
        assertEquals(Error.InvalidEncoding, Base64Pbkdf2.decode("EA2zjEJAQWeXkj6FQw/duYZxBGZfn0FZxjbEEEVvpuY\n", ByteArray(1024)).error)
    }

    @Test fun rejectTrailingEquals() {
        assertEquals(Error.InvalidEncoding, Base64Pbkdf2.decode("EA2zjEJAQWeXkj6FQw/duYZxBGZfn0FZxjbEEEVvpuY=", ByteArray(1024)).error)
    }

    @Test fun rejectNonCanonicalEncoding() {
        assertEquals(Error.InvalidEncoding, Base64Pbkdf2.decode("Mi", ByteArray(8)).error)
    }
}
