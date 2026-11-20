package io.github.kotlinmania.base64ct

import io.github.kotlinmania.base64ct.alphabet.standard.Base64
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class UsageExampleTest {
    @Test fun allocatingExample() {
        val bytes = "example bytestring!".encodeToByteArray()
        val encoded = Base64.encodeString(bytes)
        assertEquals("ZXhhbXBsZSBieXRlc3RyaW5nIQ==", encoded)
        assertContentEquals(bytes, Base64.decodeVec(encoded).success())
    }

    @Test fun callerProvidedBufferExample() {
        val bytes = "example bytestring!".encodeToByteArray()
        assertTrue(Base64.encodedLen(bytes) <= 128)
        val encoded = Base64.encode(bytes, ByteArray(128)).success()
        assertEquals("ZXhhbXBsZSBieXRlc3RyaW5nIQ==", encoded)
        assertContentEquals(bytes, Base64.decode(encoded, ByteArray(128)).success().toByteArray())
    }
}
