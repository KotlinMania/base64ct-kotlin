import Foundation
import Testing
import Base64ct
import KotlinStdlib

@Suite("Base64ct Swift Export Tests")
struct Base64ctExportTests {
    private func bytes(_ values: [UInt8]) throws -> kotlin.ByteArray {
        let decoded = alphabet.standard.Base64.shared.decodeVec(input: Data(values).base64EncodedString())
        #expect(decoded.isSuccess)
        #expect(decoded.error == nil)
        return try #require(decoded.value)
    }

    @Test("Standard alphabet encodes and decodes known bytes")
    func standardRoundTrip() throws {
        let raw = try bytes([1, 2, 3, 4])
        let base64 = alphabet.standard.Base64.shared
        #expect(base64.encodeString(input: raw) == "AQIDBA==")
        #expect(base64.encodedLen(bytes: raw) == 8)
        let result = base64.decodeVec(input: "AQIDBA==")
        let decoded = try #require(result.value)
        #expect(decoded.size == 4)
        #expect(decoded[0] == 1)
        #expect(decoded[3] == 4)
    }

    @Test("URL and password alphabets retain their distinct byte orders")
    func alphabetVariants() throws {
        let raw = try bytes([255, 239, 254])
        #expect(alphabet.url.Base64Url.shared.encodeString(input: raw) == "_-_-")
        let stars = try bytes([42, 42, 42])
        #expect(alphabet.bcrypt.Base64Bcrypt.shared.encodeString(input: stars) == "Igmo")
        #expect(alphabet.crypt.Base64Crypt.shared.encodeString(input: stars) == "8Wce")
        #expect(alphabet.shacrypt.Base64ShaCrypt.shared.encodeString(input: stars) == "ecW8")
        #expect(alphabet.pbkdf2.Base64Pbkdf2.shared.encodeString(input: raw) == "/./.")
        let one = try bytes([0])
        #expect(alphabet.standard.Base64Unpadded.shared.encodeString(input: one) == "AA")
        #expect(alphabet.url.Base64UrlUnpadded.shared.encodeString(input: one) == "AA")
    }

    @Test("Invalid padding and insufficient buffers return distinct diagnostics")
    func errorDistinctions() throws {
        let invalid = alphabet.standard.Base64.shared.decodeVec(input: "AA/=")
        #expect(invalid.error == errors.Error.InvalidEncoding)
        #expect(invalid.value == nil)
        let small = try bytes([0])
        let shortBuffer = alphabet.standard.Base64.shared.decode(src: "AAAA", dst: small)
        #expect(shortBuffer.error == errors.Error.InvalidLength)
        #expect(alphabet.standard.Base64Unpadded.shared.decodeVec(input: "AA==").error == errors.Error.InvalidEncoding)
    }

    @Test("Incremental encoder and decoder retain partial blocks")
    func incrementalRoundTrip() throws {
        let output = try bytes(Array(repeating: 0, count: 16))
        let made = encoder.Encoder.Companion.shared.new(output: output, encoding: alphabet.standard.Base64.shared)
        let encoderValue = try #require(made.value)
        #expect(encoderValue.encode(input: try bytes([1])).isSuccess)
        #expect(encoderValue.position() == 0)
        #expect(encoderValue.encode(input: try bytes([2, 3, 4])).isSuccess)
        #expect(encoderValue.finish().value == "AQIDBA==")
        let input = try bytes(Array("AQIDBA==".utf8))
        let created = decoder.Decoder.Companion.shared.new(input: input, encoding: alphabet.standard.Base64.shared)
        let decoderValue = try #require(created.value)
        let first = try bytes([0])
        #expect(decoderValue.decode(out: first).isSuccess)
        #expect(first[0] == 1)
        #expect(decoderValue.remainingLen() == 3)
        let rest = try bytes([0, 0, 0])
        #expect(decoderValue.decode(out: rest).isSuccess)
        #expect(rest[0] == 2)
        #expect(rest[2] == 4)
        #expect(decoderValue.isFinished())
    }

    @Test("Wrapped encoder preserves writable remaining capacity")
    func wrappedOutputAndBufferView() throws {
        let output = try bytes(Array(repeating: 0, count: 16))
        let made = encoder.Encoder.Companion.shared.newWrapped(
            output: output, width: 4, ending: lineending.LineEnding.CRLF,
            encoding: alphabet.standard.Base64.shared
        )
        let encoderValue = try #require(made.value)
        #expect(encoderValue.encode(input: try bytes([1, 2, 3, 4])).isSuccess)
        let finished = try #require(encoderValue.finishWithRemaining().value)
        #expect(finished.base64 == "AQID\r\nBA==")
        #expect(finished.remaining.size == 6)
        finished.remaining[0] = 42
        #expect(output[10] == 42)
    }

    @Test("Arithmetic alphabet conversion maps all six-bit values")
    func arithmeticAlphabet() {
        let expected = Array("ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789+/".utf8)
        let base64 = alphabet.standard.Base64.shared
        for value in 0..<64 {
            let character = base64.encode6bits(src: Int32(value))
            #expect(UInt8(bitPattern: character) == expected[value])
            #expect(base64.decode6bits(src: character) == Int32(value))
        }
        #expect(base64.decode6bits(src: 33) == -1)
    }

    @Test("Error display strings and conversions match Rust diagnostics")
    func errorConversionsAndDisplay() {
        #expect(errors.InvalidEncodingError.shared.fmt() == "invalid Base64 encoding")
        #expect(errors.InvalidLengthError.shared.fmt() == "invalid Base64 length")
        #expect(errors.Error.InvalidEncoding.fmt() == "invalid Base64 encoding")
        #expect(errors.Error.InvalidLength.fmt() == "invalid Base64 length")
        #expect(errors.Error.Companion.shared.from(error: errors.InvalidEncodingError.shared) == errors.Error.InvalidEncoding)
        #expect(errors.Error.Companion.shared.from(error: errors.InvalidLengthError.shared) == errors.Error.InvalidLength)
    }

    @Test("Line endings expose byte sequences and platform default")
    func lineEndingMetadata() {
        let defaultEnding = lineending.LineEnding.Companion.shared.default()
        #expect(defaultEnding == lineending.LineEnding.LF)
        let cr = lineending.LineEnding.CR.asBytes()
        #expect(lineending.LineEnding.CR.len() == 1)
        #expect(cr.size == 1 && cr[0] == 13)
        let lf = lineending.LineEnding.LF.asBytes()
        #expect(lineending.LineEnding.LF.len() == 1)
        #expect(lf.size == 1 && lf[0] == 10)
        let crlf = lineending.LineEnding.CRLF.asBytes()
        #expect(lineending.LineEnding.CRLF.len() == 2)
        #expect(crlf.size == 2 && crlf[0] == 13 && crlf[1] == 10)
    }

    @Test("In-place decoding and encoder write/flush operate cleanly")
    func decodeInPlaceAndWriterFlush() throws {
        let inplace = try bytes(Array("AQID".utf8))
        let decoded = try #require(alphabet.standard.Base64.shared.decodeInPlace(buf: inplace).value)
        #expect(decoded.size == 3)
        #expect(decoded[0] == 1 && decoded[1] == 2 && decoded[2] == 3)
        let output = try bytes(Array(repeating: 0, count: 8))
        let encoderValue = try #require(encoder.Encoder.Companion.shared.new(output: output, encoding: alphabet.standard.Base64.shared).value)
        let written = encoderValue.write(buf: try bytes([1, 2]))
        #expect(written.isSuccess)
        #expect(written.value == 2)
        #expect(encoderValue.flush().isSuccess)
        #expect(encoderValue.finish().value == "AQI=")
    }
}
