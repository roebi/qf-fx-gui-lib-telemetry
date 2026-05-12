# qf-fx-gui-lib-telemetry v1.0.0

First release of the OpenTelemetry instrumentation library for QFTest JavaFX GUI Library
performance measurement.

## What this library does

QFTest controls JavaFX applications via a Java agent. Between a test step firing on the
**server JVM** and the FX event completing on a **client JVM**, there are five distinct
phases that can each be a performance bottleneck. This library puts an OTel span around
each of them so you can see in Jaeger exactly where time is spent.

```
QFTest Server JVM                     Client JVM (one per SUT)
-----------------                     ------------------------
[1] resolver.lookup                   [3] fx.queue.wait
[2] agent.command.roundtrip  ------>  [4] fx.event.dispatch
                                      [5] fx.verification.wait
```

Spans from both JVMs are correlated in Jaeger via the `qf.step.id` attribute
(W3C trace propagation cannot cross the agent protocol boundary).

## Getting started

Add to your GUI Library project:

```kotlin
// build.gradle.kts
dependencies {
    implementation("io.github.roebi:qf-fx-gui-lib-telemetry:1.0.0")
}
```

Start Jaeger locally (OTLP/HTTP on port 4318):

```bash
podman run --rm -d --name jaeger \
  -p 16686:16686 -p 4318:4318 \
  jaegertracing/all-in-one:latest
```

Bootstrap in your resolver registration hook (server JVM):

```java
ServerTelemetry.init();  // default: http://localhost:4318/v1/traces
```

Bootstrap in your client init hook (client JVM, called once per SUT):

```java
ClientTelemetry.init(qftestClientId);  // e.g. "MyApp_1"
```

Then wrap the five measurement points - see README for the full usage sequence.

## Requirements

- Java 17+
- QFTest with a JavaFX SUT
- Jaeger (or any OTLP/HTTP collector) for viewing results

## What to look for

| Span | Attribute | Indicates |
|---|---|---|
| `resolver.lookup` | duration | Resolver registry not O(1) |
| `agent.command.roundtrip` | duration | Command serialization or IPC cost |
| `fx.queue.wait` | `fx.queue.wait_ms` | FAT overloaded / too many `runLater` calls |
| `fx.event.dispatch` | duration | Heavy scene graph traversal |
| `fx.verification.wait` | `fx.verification.polls` | Poll interval miscalibrated |

## Maven coordinates

```
io.github.roebi:qf-fx-gui-lib-telemetry:1.0.0
```

## Dependencies

- `io.opentelemetry:opentelemetry-api:1.40.0` (exposed as `api`)
- `io.opentelemetry:opentelemetry-sdk:1.40.0`
- `io.opentelemetry:opentelemetry-exporter-otlp:1.40.0`
