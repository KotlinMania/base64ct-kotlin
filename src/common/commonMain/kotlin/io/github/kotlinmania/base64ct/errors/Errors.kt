// port-lint: source src/errors.rs
package io.github.kotlinmania.base64ct.errors

private const val INVALID_ENCODING_MSG = "invalid Base64 encoding"
private const val INVALID_LENGTH_MSG = "invalid Base64 length"

/** Insufficient output buffer length. */
data object InvalidLengthError {
    override fun toString(): String = INVALID_LENGTH_MSG

    fun toError(): Error = Error.InvalidLength
}

/** Invalid encoding of the provided Base64 string. */
data object InvalidEncodingError {
    override fun toString(): String = INVALID_ENCODING_MSG

    fun toError(): Error = Error.InvalidEncoding
}

/** An invalid encoding or an insufficient output buffer. */
enum class Error {
    InvalidEncoding,
    InvalidLength,
    ;

    override fun toString(): String =
        when (this) {
            InvalidEncoding -> INVALID_ENCODING_MSG
            InvalidLength -> INVALID_LENGTH_MSG
        }
}
