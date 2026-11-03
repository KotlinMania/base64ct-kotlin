package io.github.kotlinmania.base64ct

import io.github.kotlinmania.base64ct.decoder.Decoder
import io.github.kotlinmania.base64ct.encoder.EncodedOutput
import io.github.kotlinmania.base64ct.encoder.Encoder
import io.github.kotlinmania.base64ct.encoding.Encoding
import io.github.kotlinmania.base64ct.errors.Error

/** The encoding/length diagnostic shared by concrete operation results. */
abstract class Base64Result internal constructor(
    val error: Error?,
) {
    val isSuccess: Boolean get() = error == null
}

class DecodeResult internal constructor(
    val value: ByteSlice?,
    error: Error?,
) : Base64Result(error)

class DecodeVectorResult internal constructor(
    val value: ByteArray?,
    error: Error?,
) : Base64Result(error)

class EncodeResult internal constructor(
    val value: String?,
    error: Error?,
) : Base64Result(error)

class ReadResult internal constructor(
    val value: Int?,
    error: Error?,
) : Base64Result(error)

class OperationResult internal constructor(
    value: Unit?,
    error: Error?,
) : Base64Result(error) {
    init {
        require((value != null) == (error == null))
    }
}

class FinishResult internal constructor(
    val value: EncodedOutput?,
    error: Error?,
) : Base64Result(error)

// generic by design: The selected alphabet remains part of the encoder's type.
class EncoderResult<E : Encoding> internal constructor(
    val value: Encoder<E>?,
    error: Error?,
) : Base64Result(error)

// generic by design: The selected alphabet remains part of the decoder's type.
class DecoderResult<E : Encoding> internal constructor(
    val value: Decoder<E>?,
    error: Error?,
) : Base64Result(error)

internal class Base64Failure(
    val diagnostic: Error,
) : RuntimeException(diagnostic.toString())

internal fun fail(error: Error): Nothing = throw Base64Failure(error)

internal inline fun <T : Any, R : Base64Result> result(factory: (T?, Error?) -> R, operation: () -> T): R =
    try {
        factory(operation(), null)
    } catch (failure: Base64Failure) {
        factory(null, failure.diagnostic)
    }

internal fun checkedAdd(a: Int, b: Int): Int {
    if (a < 0 || b < 0 || a > Int.MAX_VALUE - b) fail(Error.InvalidLength)
    return a + b
}

internal fun checkedSub(a: Int, b: Int): Int {
    if (b < 0 || a < b) fail(Error.InvalidLength)
    return a - b
}
