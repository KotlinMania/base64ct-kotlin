// port-lint: tests tests/proptests.rs
package io.github.kotlinmania.base64ct

import io.github.kotlinmania.base64ct.alphabet.standard.Base64
import io.github.kotlinmania.base64ct.alphabet.standard.Base64Unpadded
import io.github.kotlinmania.base64ct.decoder.Decoder
import io.github.kotlinmania.base64ct.encoder.Encoder
import kotlin.random.Random
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlin.io.encoding.Base64 as ReferenceBase64

class Proptests {
    @Test fun decodeEquiv() {
        val random = Random(103)
        for (size in 0..256) {
            val bytes = random.nextBytes(size)
            assertContentEquals(bytes, Base64.decodeVec(ReferenceBase64.encode(bytes)).success())
        }
    }

    @Test fun decodeIncremental() {
        val random = Random(104)
        for (size in 1..256) {
            val bytes = random.nextBytes(size)
            val decoder = Decoder.new(ReferenceBase64.encode(bytes).encodeToByteArray(), Base64).success()
            checkChunks(bytes, random.nextInt(1, 256) % size + 1, decoder)
        }
    }

    @Test fun decodeIncrementalWrapped() {
        val random = Random(105)
        for (width in 4 until 128) {
            val bytes = random.nextBytes(random.nextInt(1, 257))
            for (ending in listOf("\r", "\n", "\r\n")) {
                val encoded = ReferenceBase64.encode(bytes).chunked(width).joinToString(ending)
                val decoder = Decoder.newWrapped(encoded.encodeToByteArray(), width, Base64).success()
                checkChunks(bytes, random.nextInt(1, 256) % bytes.size + 1, decoder)
            }
        }
    }

    @Test fun decodeRandom() {
        val random = Random(106)
        val alphabet = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789+/"
        repeat(1024) {
            val text = randomText(random, alphabet)
            val padded = text + "=".repeat((4 - text.length % 4) % 4)
            assertContentEquals(referenceDecode(padded, ReferenceBase64), Base64.decodeVec(padded).value)
        }
    }

    @Test fun encodeEquiv() {
        val random = Random(107)
        for (size in 0..256) {
            val bytes = random.nextBytes(size)
            assertEquals(ReferenceBase64.encode(bytes), Base64.encodeString(bytes))
        }
    }

    @Test fun encodeIncremental() {
        val random = Random(108)
        for (size in 1..256) {
            val bytes = random.nextBytes(size)
            val encoder = Encoder.new(ByteArray(1024), Base64).success()
            val chunkSize = random.nextInt(1, 256) % size + 1
            for (offset in bytes.indices step chunkSize) {
                encoder.encode(bytes.copyOfRange(offset, minOf(size, offset + chunkSize))).success()
            }
            assertEquals(ReferenceBase64.encode(bytes), encoder.finish().success())
        }
    }

    @Test fun decodeArbitraryPadded() {
        val random = Random(109)
        repeat(1024) {
            val text = randomText(random, "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789/+=?")
            assertContentEquals(referenceDecode(text, ReferenceBase64), Base64.decodeVec(text).value)
        }
    }

    @Test fun decodeArbitraryUnpadded() {
        val random = Random(110)
        val reference = ReferenceBase64.withPadding(ReferenceBase64.PaddingOption.ABSENT)
        repeat(1024) {
            val text = randomText(random, "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789/+=?")
            assertContentEquals(referenceDecode(text, reference), Base64Unpadded.decodeVec(text).value)
        }
    }

    private fun randomText(random: Random, alphabet: String): String =
        buildString {
            repeat(random.nextInt(257)) { append(alphabet[random.nextInt(alphabet.length)]) }
        }

    private fun referenceDecode(text: String, reference: ReferenceBase64): ByteArray? =
        try {
            reference.decode(text)
        } catch (_: IllegalArgumentException) {
            null
        }

    private fun checkChunks(bytes: ByteArray, chunkSize: Int, decoder: Decoder<Base64>) {
        var remaining = decoder.remainingLen()
        for (offset in bytes.indices step chunkSize) {
            val chunk = bytes.copyOfRange(offset, minOf(bytes.size, offset + chunkSize))
            assertFalse(decoder.isFinished())
            val decoded = decoder.decode(ByteArray(chunk.size)).success().toByteArray()
            assertContentEquals(chunk, decoded)
            remaining -= decoded.size
            assertEquals(remaining, decoder.remainingLen())
        }
        assertTrue(decoder.isFinished())
        assertEquals(0, decoder.remainingLen())
    }
}
