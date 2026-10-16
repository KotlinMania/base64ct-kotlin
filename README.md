# base64ct-kotlin in Kotlin

[![GitHub link](https://img.shields.io/badge/GitHub-KotlinMania%2Fbase64ct-kotlin-blue.svg)](https://github.com/KotlinMania/base64ct-kotlin)
[![Maven Central](https://img.shields.io/maven-central/v/io.github.kotlinmania/base64ct-kotlin)](https://central.sonatype.com/artifact/io.github.kotlinmania/base64ct-kotlin)
[![Build status](https://img.shields.io/github/actions/workflow/status/KotlinMania/base64ct-kotlin/ci.yml?branch=main)](https://github.com/KotlinMania/base64ct-kotlin/actions)

This is a Kotlin Multiplatform line-by-line transliteration port of [`RustCrypto/formats`](https://github.com/RustCrypto/formats) (crate `base64ct`).

**Original Project:** This port is based on [`RustCrypto/formats`](https://github.com/RustCrypto/formats). All design credit and project intent belong to the upstream authors; this repository is a faithful port to Kotlin Multiplatform with no behavioural changes intended.

### Porting status

This is an **in-progress port**. The goal is feature parity with the upstream Rust crate while providing a native Kotlin Multiplatform API. Every Kotlin file carries a `// port-lint: source <path>` header naming its upstream Rust counterpart so the AST-distance tool can track provenance.

---

## Upstream README — `RustCrypto/formats`

> The text below is reproduced from [`https://github.com/RustCrypto/formats`](https://github.com/RustCrypto/formats). It is the upstream project's own description and remains under the upstream authors' authorship; links have been rewritten to absolute upstream URLs where appropriate so they continue to resolve from this repository.

# [RustCrypto]: Constant-Time Base64

[![crate][crate-image]][crate-link]
[![Docs][docs-image]][docs-link]
[![Build Status][build-image]][build-link]
![Apache2/MIT licensed][license-image]
![Rust Version][rustc-image]
[![Project Chat][chat-image]][chat-link]

Pure Rust implementation of Base64 ([RFC 4648]).

Implements multiple Base64 alphabets without data-dependent branches or lookup
tables, thereby providing portable "best effort" constant-time operation.

Supports `no_std` environments and avoids heap allocations in the core API
(but also provides optional `alloc` support for convenience).

[Documentation][docs-link]

## About

This crate implements several Base64 alphabets in constant-time for sidechannel
resistance, aimed at purposes like encoding/decoding the "PEM" format used to
store things like cryptographic private keys (i.e. in the [`pem-rfc7468`] crate).

The paper [Util::Lookup: Exploiting key decoding in cryptographic libraries][Util::Lookup]
demonstrates how the leakage from non-constant-time Base64 parsers can be used
to practically extract RSA private keys from SGX enclaves.

The padded variants require (`=`) padding. Unpadded variants expressly
reject such padding.

Whitespace is expressly disallowed, with the exception of the
[`Decoder::new_wrapped`] and [`Encoder::new_wrapped`] modes which provide
fixed-width line wrapping.

## Supported Base64 variants

- Standard Base64: `[A-Z]`, `[a-z]`, `[0-9]`, `+`, `/`
- URL-safe Base64: `[A-Z]`, `[a-z]`, `[0-9]`, `-`, `_`
- bcrypt Base64: `.`, `/`, `[A-Z]`, `[a-z]`, `[0-9]`
- `crypt(3)` Base64: `.`, `-`, `[0-9]`, `[A-Z]`, `[a-z]`
- PBKDF2 Base64: `[A-Z]`, `[a-z]`, `[0-9]`, `.`, `/`

## Minimum Supported Rust Version (MSRV) Policy

MSRV increases are not considered breaking changes and can happen in patch releases.

The crate MSRV accounts for all supported targets and crate feature combinations, excluding
explicitly unstable features.

## License

Licensed under either of:

 * [Apache License, Version 2.0](http://www.apache.org/licenses/LICENSE-2.0)
 * [MIT license](http://opensource.org/licenses/MIT)

at your option.

### Contribution

Unless you explicitly state otherwise, any contribution intentionally submitted
for inclusion in the work by you, as defined in the Apache-2.0 license, shall be
dual licensed as above, without any additional terms or conditions.

[//]: # (badges)

[crate-image]: https://img.shields.io/crates/v/base64ct?logo=rust
[crate-link]: https://crates.io/crates/base64ct
[docs-image]: https://docs.rs/base64ct/badge.svg
[docs-link]: https://docs.rs/base64ct/
[build-image]: https://github.com/RustCrypto/formats/actions/workflows/base64ct.yml/badge.svg
[build-link]: https://github.com/RustCrypto/formats/actions/workflows/base64ct.yml
[license-image]: https://img.shields.io/badge/license-Apache2.0/MIT-blue.svg
[rustc-image]: https://img.shields.io/badge/rustc-1.85+-blue.svg
[chat-image]: https://img.shields.io/badge/zulip-join_chat-blue.svg
[chat-link]: https://rustcrypto.zulipchat.com/#narrow/stream/300570-formats

[//]: # (links)

[RustCrypto]: https://github.com/rustcrypto
[RFC 4648]: https://tools.ietf.org/html/rfc4648
[`pem-rfc7468`]: https://github.com/RustCrypto/formats/tree/master/pem-rfc7468
[Util::Lookup]: https://arxiv.org/pdf/2108.04600.pdf
[`Decoder::new_wrapped`]: https://docs.rs/base64ct/latest/base64ct/struct.Decoder.html#method.new_wrapped
[`Encoder::new_wrapped`]: https://docs.rs/base64ct/latest/base64ct/struct.Encoder.html#method.new_wrapped

---

## About this Kotlin port

### Installation

```kotlin
dependencies {
    implementation("io.github.kotlinmania:base64ct-kotlin:0.1.0")
}
```

### Building

```bash
./gradlew build
./gradlew test
```

### Targets

- macOS arm64
- Linux x64
- Windows mingw-x64
- iOS arm64 / simulator-arm64 (Swift export + XCFramework)
- JS (browser + Node.js)
- Wasm-JS (browser + Node.js)
- Android (API 24+)

### Porting guidelines

See [AGENTS.md](AGENTS.md) and [CLAUDE.md](CLAUDE.md) for translator discipline, port-lint header convention, and Rust → Kotlin idiom mapping.

### License

This Kotlin port is distributed under the same license terms as the upstream [`RustCrypto/formats`](https://github.com/RustCrypto/formats). See [LICENSE](LICENSE) (and any sibling `LICENSE-*` / `NOTICE` files mirrored from upstream) for the full text.

Original work copyrighted by the base64ct authors.  
Kotlin port: Copyright (c) 2026 Sydney Renee and The Solace Project.

### Acknowledgments

Thanks to the [`RustCrypto/formats`](https://github.com/RustCrypto/formats) maintainers and contributors for the original Rust implementation. This port reproduces their work in Kotlin Multiplatform; bug reports about upstream design or behavior should go to the upstream repository.
