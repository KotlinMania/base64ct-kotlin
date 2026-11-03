package io.github.kotlinmania.base64ct

import io.github.kotlinmania.base64ct.decoder.Decoder
import io.github.kotlinmania.base64ct.encoder.EncodedOutput
import io.github.kotlinmania.base64ct.encoder.Encoder
import io.github.kotlinmania.base64ct.encoding.Encoding
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

internal class TestVector(
    val raw: ByteArray,
    val b64: String,
)

internal fun hex(value: String): ByteArray {
    require(value.length % 2 == 0)
    return ByteArray(value.length / 2) { index -> value.substring(index * 2, index * 2 + 2).toInt(16).toByte() }
}

internal fun DecodeResult.success(): ByteSlice {
    assertNull(error)
    return assertNotNull(value)
}

internal fun DecodeVectorResult.success(): ByteArray {
    assertNull(error)
    return assertNotNull(value)
}

internal fun EncodeResult.success(): String {
    assertNull(error)
    return assertNotNull(value)
}

internal fun ReadResult.success(): Int {
    assertNull(error)
    return assertNotNull(value)
}

internal fun OperationResult.success() {
    assertNull(error)
}

internal fun FinishResult.success(): EncodedOutput {
    assertNull(error)
    return assertNotNull(value)
}

internal fun <E : Encoding> DecoderResult<E>.success(): Decoder<E> {
    assertNull(error)
    return assertNotNull(value)
}

internal fun <E : Encoding> EncoderResult<E>.success(): Encoder<E> {
    assertNull(error)
    return assertNotNull(value)
}

internal fun vectors(vararg encoded: String): List<TestVector> {
    val raw =
        listOf(
            "",
            "00",
            "2a2a2a",
            "01020304",
            "adadadadad",
            "ffeffeffeffe",
            "ffffffffff",
            "40c13fbd054c722aa3c2f21173c069ea497d35296bcc2465f6f9d041087bd7a9",
            "00108310518720928b30d38f41149351559761969b71d79f8218a39259a7a29aabb2dbafc31cb3fbf000",
        )
    require(encoded.size == raw.size)
    return raw.indices.map { TestVector(hex(raw[it]), encoded[it]) }
}

internal fun encodeTestVectors(encoding: Encoding, vectors: List<TestVector>) {
    val buffer = ByteArray(1024)
    for (vector in vectors) {
        assertEquals(vector.b64, encoding.encode(vector.raw, buffer).success())
        assertEquals(vector.b64.length, encoding.encodedLen(vector.raw))
        assertEquals(vector.b64, encoding.encodeString(vector.raw))
    }
}

internal fun decodeTestVectors(encoding: Encoding, vectors: List<TestVector>) {
    val buffer = ByteArray(1024)
    for (vector in vectors) {
        assertContentEquals(vector.raw, encoding.decode(vector.b64, buffer).success().toByteArray())
        assertContentEquals(vector.raw, encoding.decodeInPlace(vector.b64.encodeToByteArray()).success().toByteArray())
        assertContentEquals(vector.raw, encoding.decodeVec(vector.b64).success())
    }
}

internal fun encodeAndDecodeVariousLengths(encoding: Encoding) {
    val data = ByteArray(64) { 'X'.code.toByte() }
    val input = ByteArray(1024)
    val output = ByteArray(1024)
    for (i in data.indices) {
        val expected = data.copyOf(i)
        val encoded = encoding.encode(expected, input).success()
        assertContentEquals(expected, encoding.decode(encoded, output).success().toByteArray())
        assertContentEquals(expected, encoding.decodeInPlace(encoded.encodeToByteArray()).success().toByteArray())
        assertContentEquals(expected, encoding.decodeVec(encoding.encodeString(expected)).success())
    }
}
