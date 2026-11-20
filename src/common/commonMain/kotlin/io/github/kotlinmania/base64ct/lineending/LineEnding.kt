// port-lint: source src/line_ending.rs
package io.github.kotlinmania.base64ct.lineending

internal const val CHAR_CR: Byte = 13
internal const val CHAR_LF: Byte = 10

/** Newline conventions supported by wrapped Base64. */
enum class LineEnding {
    CR,
    LF,
    CRLF,
    ;

    fun asBytes(): ByteArray =
        when (this) {
            CR -> byteArrayOf(CHAR_CR)
            LF -> byteArrayOf(CHAR_LF)
            CRLF -> byteArrayOf(CHAR_CR, CHAR_LF)
        }

    fun len(): Int = asBytes().size

    companion object {
        fun default(): LineEnding = platformLineEnding()
    }
}

internal expect fun platformLineEnding(): LineEnding
