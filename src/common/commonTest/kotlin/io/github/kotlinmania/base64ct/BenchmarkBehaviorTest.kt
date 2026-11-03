package io.github.kotlinmania.base64ct

import io.github.kotlinmania.base64ct.alphabet.standard.Base64Unpadded
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.io.encoding.Base64 as ReferenceBase64

class BenchmarkBehaviorTest {
    private val b64Len = 100_000
    private val rawLen = 3 * b64Len / 4

    private fun getRawData(): ByteArray = ByteArray(rawLen) { it.toByte() }

    private fun getB64Data(): String =
        buildString {
            repeat(b64Len) {
                val value = it % 64
                append(
                    when (value) {
                        in 0..25 -> ('A'.code + value).toChar()
                        in 26..51 -> ('a'.code + value - 26).toChar()
                        in 52..61 -> ('0'.code + value - 52).toChar()
                        62 -> '+'
                        else -> '/'
                    },
                )
            }
        }

    @Test fun decodeBenchInput() {
        val input = getB64Data()
        assertContentEquals(ReferenceBase64.decode(input), Base64Unpadded.decode(input, getRawData()).success().toByteArray())
    }

    @Test fun decodeInPlaceBenchInput() {
        val input = getB64Data()
        assertContentEquals(ReferenceBase64.decode(input), Base64Unpadded.decodeInPlace(input.encodeToByteArray()).success().toByteArray())
    }

    @Test fun encodeBenchInput() {
        val input = getRawData()
        assertEquals(ReferenceBase64.encode(input), Base64Unpadded.encode(input, getB64Data().encodeToByteArray()).success())
    }
}
