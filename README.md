# qf-fx-gui-lib-telemetry

OpenTelemetry instrumentation library for QFTest FX GUI Library performance measurement.

## Architecture

```
QFTest Server JVM  (one per test run)
  ServerTelemetry.init()  ->  ServerSpans.*

  <-- agent protocol (black box) -->

Client JVM 1..N  (one per SUT instance)
  ClientTelemetry.init(clientId)  ->  ClientSpans.*
```

## The 5 Measurement Points

| # | Span name | JVM | Class |
|---|---|---|---|
| 1 | `resolver.lookup` | Server | `ServerSpans.resolverLookup()` |
| 2 | `agent.command.roundtrip` | Server | `ServerSpans.agentRoundTrip()` |
| 3 | `fx.queue.wait` | Client | `ClientSpans.startQueueWait()` |
| 4 | `fx.event.dispatch` | Client | `ClientSpans.fxEventDispatch()` |
| 5 | `fx.verification.wait` | Client | `ClientSpans.verificationWait()` |

Cross-JVM correlation is done via the `qf.step.id` attribute (not W3C trace propagation,
which cannot cross the QFTest agent protocol boundary).

## Prerequisites

- Java 21+
- Jaeger running locally (see below)

## Build

```bash
./gradlew build
./gradlew test
```

## Run Jaeger locally

```bash
podman run --rm -d \
  --name jaeger \
  -p 16686:16686 \
  -p 4318:4318 \
  jaegertracing/all-in-one:latest
```

Open http://localhost:16686 after the test run.

Note: this library uses OTLP/HTTP (port 4318), not gRPC (port 4317),
to keep the dependency tree simple.

## Usage - Server JVM

Call once during suite initialisation (resolver registration hook):

```java
// default endpoint: http://localhost:4318/v1/traces
ServerTelemetry.init();

// or custom endpoint:
ServerTelemetry.init("http://my-otel-collector:4318/v1/traces");
```

Instrument resolver lookup:

```java
try (ActiveSpan s = ServerSpans.resolverLookup(clientId, componentId, stepId)) {
    return resolverRegistry.resolve(componentId);
} catch (Exception e) {
    // s.recordException(e) is called automatically if you rethrow
    throw e;
}
```

Instrument agent round-trip:

```java
try (ActiveSpan s = ServerSpans.agentRoundTrip(clientId, "click")) {
    return agentChannel.sendAndAwaitAck(clientId, command);
}
```

Shutdown in suite teardown:

```java
ServerTelemetry.shutdown();
```

## Usage - Client JVM

Call once in your client-side init hook:

```java
ClientTelemetry.init(qftestClientId);   // e.g. "MyApp_1"
```

Instrument Platform.runLater() queue wait + FX event dispatch:

```java
QueueWaitMark mark = ClientSpans.startQueueWait(componentId, stepId);
Platform.runLater(() -> {
    mark.arrived();    // records fx.queue.wait_ms, ends that span
    try (ActiveSpan s = ClientSpans.fxEventDispatch(componentId, stepId, eventType)) {
        node.fireEvent(event);
    }
});
```

Instrument verification / polling wait:

```java
try (VerificationSpan vs = ClientSpans.verificationWait(componentId, stepId)) {
    while (!stateCondition.isMet()) {
        vs.incrementPollCount();
        Thread.sleep(pollIntervalMs);
    }
}
// fx.verification.polls is recorded automatically on close
```

Shutdown when client is stopped:

```java
ClientTelemetry.shutdown();
```

## Interpreting Results in Jaeger

Search by tag `qf.step.id=<value>` to correlate server and client spans for the same test step.

| Finding | Likely cause | Fix |
|---|---|---|
| `resolver.lookup` slow | Linear scan of resolver list | HashMap by component type |
| `agent.command.roundtrip` slow | Serialization or IPC latency | Batch commands |
| `fx.queue.wait_ms` high | FAT overloaded | Reduce `runLater` frequency |
| `fx.event.dispatch` slow | Heavy scene graph traversal | Cache Node references |
| `fx.verification.polls` high | Poll interval too short | Adaptive backoff |

## Adding to Your Project

After building, the jar is at `build/libs/qf-fx-gui-lib-telemetry-1.0.0.jar`.

Add to your GUI Library's build:

```kotlin
// build.gradle.kts
dependencies {
    implementation(files("libs/qf-fx-gui-lib-telemetry-1.0.0.jar"))
    // OTel SDK + exporter (transitive via the lib, or declare explicitly)
}
```
