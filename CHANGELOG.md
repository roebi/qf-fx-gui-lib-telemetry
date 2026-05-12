# Changelog

All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.1.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [1.0.0] - 2026-05-12

### Added

- `ServerTelemetry` - OTel SDK bootstrap for the QFTest server JVM with idempotent
  `init()` / `shutdown()` and configurable OTLP/HTTP endpoint
- `ClientTelemetry` - OTel SDK bootstrap for QFTest client JVMs; takes the QFTest
  `clientId` as a resource attribute so all client spans are tagged by SUT instance
- `ServerSpans` - span factory covering two server-side measurement points:
  - `resolver.lookup` - time to resolve a component in the resolver registry
  - `agent.command.roundtrip` - time from command send to client ACK
- `ClientSpans` - span factory covering three client-side measurement points:
  - `fx.queue.wait` - time a `Platform.runLater()` call waits in the JavaFX
    Application Thread queue (cross-thread safe, no Scope leak)
  - `fx.event.dispatch` - time to execute the FX event on the FAT
  - `fx.verification.wait` - time and poll count for state assertion / polling wait
- `ActiveSpan` - `AutoCloseable` span wrapper for try-with-resources usage
- `QueueWaitMark` - cross-thread queue wait holder; started on the calling thread,
  ended on the FAT via `arrived()`
- `VerificationSpan` - `AutoCloseable` span with poll count accumulation;
  records `fx.verification.polls` automatically on close
- 34 unit tests using `InMemorySpanExporter` - no Jaeger required to run the test suite
- Gradle 9 build with Kotlin DSL and version catalog (`gradle/libs.versions.toml`)
- Publishing to Maven Central via `com.gradleup.nmcp` settings plugin (Central Portal
  bundle upload API - not the legacy OSSRH PUT protocol)
- GPG signing with in-memory key support for CI (`useInMemoryPgpKeys`)
- OWASP Dependency Check v10 supply-chain audit gate (fails on CVSS >= 7.0)
- GitHub Actions: `ci.yml` (test on push/PR) and `publish.yml` (publish on version tag)
- NVD data cache in CI to avoid re-downloading the full vulnerability database

### Architecture note

Cross-JVM correlation (server spans vs client spans) is done via the shared
`qf.step.id` attribute rather than W3C `traceparent` propagation, because the
QFTest agent protocol boundary is opaque and cannot carry trace context headers.
Query `qf.step.id=<value>` in Jaeger to see both JVMs' spans for the same test step.

[1.0.0]: https://github.com/roebi/qf-fx-gui-lib-telemetry/releases/tag/v1.0.0
