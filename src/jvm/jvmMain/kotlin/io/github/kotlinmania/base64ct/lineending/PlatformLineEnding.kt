package io.github.kotlinmania.base64ct.lineending

internal actual fun platformLineEnding(): LineEnding =
    if (System.lineSeparator() == "\r\n") LineEnding.CRLF else LineEnding.LF
