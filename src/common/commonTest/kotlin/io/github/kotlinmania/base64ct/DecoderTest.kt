// port-lint: tests src/decoder.rs
package io.github.kotlinmania.base64ct

import io.github.kotlinmania.base64ct.alphabet.standard.Base64
import io.github.kotlinmania.base64ct.alphabet.standard.Base64Unpadded
import io.github.kotlinmania.base64ct.decoder.Decoder
import io.github.kotlinmania.base64ct.encoding.Encoding
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class DecoderTest {
    @Test fun decodePadded() = decodeTest(PADDED_BIN) { Decoder.new(PADDED_BASE64.encodeToByteArray(), Base64).success() }

    @Test fun decodeUnpadded() = decodeTest(UNPADDED_BIN) { Decoder.new(UNPADDED_BASE64.encodeToByteArray(), Base64Unpadded).success() }

    @Test fun decodeMultilinePadded() =
        decodeTest(MULTILINE_PADDED_BIN) {
            Decoder.newWrapped(MULTILINE_PADDED_BASE64.encodeToByteArray(), 70, Base64).success()
        }

    @Test fun decodeMultilineUnpadded() =
        decodeTest(MULTILINE_UNPADDED_BIN) {
            Decoder.newWrapped(MULTILINE_UNPADDED_BASE64.encodeToByteArray(), 70, Base64Unpadded).success()
        }

    @Test fun readMultilinePadded() {
        val decoder = Decoder.newWrapped(MULTILINE_PADDED_BASE64.encodeToByteArray(), 70, Base64).success()
        val buffer = mutableListOf<Byte>()
        assertEquals(MULTILINE_PADDED_BIN.size, decoder.readToEnd(buffer).success())
        assertContentEquals(MULTILINE_PADDED_BIN, buffer.toByteArray())
    }

    @Test fun decodeEmptyAtEnd() {
        val decoder = Decoder.new("AAAA".encodeToByteArray(), Base64).success()
        assertContentEquals(ByteArray(3), decoder.decode(ByteArray(3)).success().toByteArray())
        assertContentEquals(ByteArray(0), decoder.decode(ByteArray(0)).success().toByteArray())
    }

    private fun <E : Encoding> decodeTest(expected: ByteArray, factory: () -> Decoder<E>) {
        for (chunkSize in 1 until expected.size) {
            val decoder = factory()
            var remainingLen = decoder.remainingLen()
            for (offset in expected.indices step chunkSize) {
                val chunk = expected.copyOfRange(offset, minOf(offset + chunkSize, expected.size))
                assertFalse(decoder.isFinished())
                val decoded = decoder.decode(ByteArray(chunk.size)).success().toByteArray()
                assertContentEquals(chunk, decoded)
                remainingLen -= decoded.size
                assertEquals(remainingLen, decoder.remainingLen())
            }
            assertTrue(decoder.isFinished())
            assertEquals(0, decoder.remainingLen())
        }
    }
}
