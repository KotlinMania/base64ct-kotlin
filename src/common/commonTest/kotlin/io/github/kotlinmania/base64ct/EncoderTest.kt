// port-lint: tests src/encoder.rs
package io.github.kotlinmania.base64ct

import io.github.kotlinmania.base64ct.alphabet.standard.Base64
import io.github.kotlinmania.base64ct.alphabet.standard.Base64Unpadded
import io.github.kotlinmania.base64ct.encoder.Encoder
import io.github.kotlinmania.base64ct.encoding.Encoding
import io.github.kotlinmania.base64ct.lineending.LineEnding
import kotlin.test.Test
import kotlin.test.assertEquals

class EncoderTest {
    @Test fun encodePadded() = encodeTest(Base64, PADDED_BIN, PADDED_BASE64, null)

    @Test fun encodeUnpadded() = encodeTest(Base64Unpadded, UNPADDED_BIN, UNPADDED_BASE64, null)

    @Test fun encodeMultilinePadded() = encodeTest(Base64, MULTILINE_PADDED_BIN, MULTILINE_PADDED_BASE64, 70)

    @Test fun encodeMultilineUnpadded() = encodeTest(Base64Unpadded, MULTILINE_UNPADDED_BIN, MULTILINE_UNPADDED_BASE64, 70)

    @Test fun noTrailingNewlineWhenAligned() {
        val encoder = Encoder.newWrapped(ByteArray(64), 64, LineEnding.LF, Base64).success()
        encoder.encode(ByteArray(48)).success()
        assertEquals("AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA", encoder.finish().success())
    }

    private fun <E : Encoding> encodeTest(encoding: E, input: ByteArray, expected: String, wrapped: Int?) {
        val buffer = ByteArray(1024)
        for (chunkSize in 1 until input.size) {
            val encoder =
                if (wrapped == null) {
                    Encoder.new(buffer, encoding).success()
                } else {
                    Encoder.newWrapped(buffer, wrapped, LineEnding.LF, encoding).success()
                }
            for (offset in input.indices step chunkSize) {
                encoder.encode(input.copyOfRange(offset, minOf(input.size, offset + chunkSize))).success()
            }
            assertEquals(expected, encoder.finish().success())
        }
    }
}
