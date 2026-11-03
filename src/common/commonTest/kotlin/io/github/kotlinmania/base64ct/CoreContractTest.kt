package io.github.kotlinmania.base64ct

import io.github.kotlinmania.base64ct.alphabet.bcrypt.Base64Bcrypt
import io.github.kotlinmania.base64ct.alphabet.crypt.Base64Crypt
import io.github.kotlinmania.base64ct.alphabet.pbkdf2.Base64Pbkdf2
import io.github.kotlinmania.base64ct.alphabet.shacrypt.Base64ShaCrypt
import io.github.kotlinmania.base64ct.alphabet.standard.Base64
import io.github.kotlinmania.base64ct.alphabet.standard.Base64Unpadded
import io.github.kotlinmania.base64ct.alphabet.url.Base64Url
import io.github.kotlinmania.base64ct.alphabet.url.Base64UrlUnpadded
import io.github.kotlinmania.base64ct.decoder.Decoder
import io.github.kotlinmania.base64ct.encoder.Encoder
import io.github.kotlinmania.base64ct.encoding.encodedLenInner
import io.github.kotlinmania.base64ct.errors.Error
import io.github.kotlinmania.base64ct.errors.InvalidEncodingError
import io.github.kotlinmania.base64ct.errors.InvalidLengthError
import io.github.kotlinmania.base64ct.lineending.LineEnding
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class CoreContractTest {
    @Test fun allByteValuesAndAlphabetArithmetic() {
        val encodings = listOf(Base64, Base64Unpadded, Base64Url, Base64UrlUnpadded, Base64Bcrypt, Base64Crypt, Base64Pbkdf2, Base64ShaCrypt)
        for (encoding in encodings) {
            val alphabet = (0..63).map { encoding.encode6bits(it) }
            for (byte in 0..255) {
                assertEquals(alphabet.indexOf(byte.toByte()), encoding.decode6bits(byte.toByte()))
            }
            val raw = ByteArray(256) { it.toByte() }
            assertContentEquals(raw, encoding.decodeVec(encoding.encodeString(raw)).success())
        }
    }

    @Test fun invalidLengthsAndPadding() {
        assertEquals(Error.InvalidLength, Base64.encode(byteArrayOf(1), ByteArray(3)).error)
        assertEquals(Error.InvalidLength, Base64.decode("AAAA", ByteArray(2)).error)
        for (input in listOf("A", "A===", "AA=A", "====", "AA", "AB==", "AAB=", "AA==\n", "\u0100AAA")) {
            assertEquals(Error.InvalidEncoding, Base64.decodeVec(input).error, input)
        }
        assertContentEquals(byteArrayOf(0), Base64.decodeInPlace("AB==".encodeToByteArray()).success().toByteArray())
        assertNull(encodedLenInner(Int.MAX_VALUE, true))
        assertEquals(0, encodedLenInner(0, false))
        assertEquals(2, encodedLenInner(1, false))
        assertEquals(3, encodedLenInner(2, false))
        assertEquals(4, encodedLenInner(3, false))
    }

    @Test fun bufferViewsRetainOriginalStorage() {
        val buffer = ByteArray(10) { 99 }
        val decoded = Base64.decode("AQID", buffer).success()
        assertContentEquals(byteArrayOf(1, 2, 3), decoded.toByteArray())
        assertEquals(99, buffer[3].toInt())
        decoded[0] = 42
        assertEquals(42, buffer[0].toInt())
        val encoder = Encoder.new(buffer, Base64).success()
        encoder.write(byteArrayOf(1)).success()
        assertEquals(0, encoder.position())
        encoder.flush().success()
        val output = encoder.finishWithRemaining().success()
        assertEquals("AQ==", output.base64)
        assertEquals(6, output.remaining.size)
        output.remaining[0] = 17
        assertEquals(17, buffer[4].toInt())
    }

    @Test fun incrementalLengthErrorsAndClone() {
        assertEquals(Error.InvalidLength, Decoder.new(ByteArray(0), Base64).error)
        assertEquals(Error.InvalidLength, Encoder.new(ByteArray(0), Base64).error)
        assertEquals(Error.InvalidLength, Decoder.newWrapped("AAAA".encodeToByteArray(), 3, Base64).error)
        assertEquals(Error.InvalidLength, Encoder.newWrapped(ByteArray(10), 3, LineEnding.LF, Base64).error)
        assertEquals(Error.InvalidEncoding, Decoder.newWrapped("AAAAAAAA".encodeToByteArray(), 4, Base64).error)
        val decoder = Decoder.new("AQIDBA==".encodeToByteArray(), Base64).success()
        assertEquals(1, decoder.read(ByteArray(1)).success())
        val cloned = decoder.clone()
        val tail = ByteArray(3)
        cloned.readExact(tail).success()
        assertContentEquals(byteArrayOf(2, 3, 4), tail)
        assertEquals(3, decoder.remainingLen())
        val appended = mutableListOf<Byte>(9)
        assertContentEquals(tail, decoder.decodeToEnd(appended).success())
        assertContentEquals(byteArrayOf(9, 2, 3, 4), appended.toByteArray())
        assertEquals(0, decoder.read(ByteArray(10)).success())
        assertEquals(0, decoder.readToEnd(appended).success())
        assertEquals(Error.InvalidLength, decoder.decode(ByteArray(1)).error)
    }

    @Test fun wrappingWithEveryNewlineAndWidth() {
        val input = ByteArray(128) { it.toByte() }
        val encoded = Base64.encodeString(input)
        for (width in 4..80) {
            for (ending in LineEnding.entries) {
                val encoder = Encoder.newWrapped(ByteArray(512), width, ending, Base64).success()
                for (byte in input) encoder.encode(byteArrayOf(byte)).success()
                val output = encoder.finish().success()
                assertEquals(encoded.chunked(width).joinToString(ending.asBytes().decodeToString()), output)
                val decoder = Decoder.newWrapped(output.encodeToByteArray(), width, Base64).success()
                assertContentEquals(input, decoder.decode(ByteArray(input.size)).success().toByteArray())
                assertTrue(decoder.isFinished())
            }
        }
    }

    @Test fun diagnosticAndLineEndingValues() {
        assertEquals("invalid Base64 encoding", InvalidEncodingError.toString())
        assertEquals("invalid Base64 length", InvalidLengthError.toString())
        assertEquals(Error.InvalidEncoding, InvalidEncodingError.toError())
        assertEquals(Error.InvalidLength, InvalidLengthError.toError())
        assertEquals("invalid Base64 encoding", Error.InvalidEncoding.toString())
        assertContentEquals(byteArrayOf(13), LineEnding.CR.asBytes())
        assertContentEquals(byteArrayOf(10), LineEnding.LF.asBytes())
        assertContentEquals(byteArrayOf(13, 10), LineEnding.CRLF.asBytes())
        assertEquals(2, LineEnding.CRLF.len())
    }
}
