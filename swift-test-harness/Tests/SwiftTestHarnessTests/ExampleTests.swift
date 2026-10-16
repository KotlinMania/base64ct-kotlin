#if canImport(Testing)
import Testing
import Base64ct

@Suite("Base64ct Swift Export Tests")
struct Base64ctExportTests {
    @Test("Base64ct swift module imported cleanly")
    func testSwiftModuleLoads() throws {
        #expect(Bool(true), "Base64ct swift module imported cleanly")
    }
}
#elseif canImport(XCTest)
import XCTest
import Base64ct

final class Base64ctExportTests: XCTestCase {
    func testSwiftModuleLoads() throws {
        XCTAssertTrue(true, "Base64ct swift module imported cleanly")
    }
}
#endif
