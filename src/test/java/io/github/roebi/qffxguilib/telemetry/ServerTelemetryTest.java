package io.github.roebi.qffxguilib.telemetry;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.assertThatCode;

class ServerTelemetryTest {

    @AfterEach
    void tearDown() {
        ServerTelemetry.resetForTesting();
    }

    @Test
    void tracer_throwsIfNotInitialised() {
        assertThatThrownBy(ServerTelemetry::tracer)
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("ServerTelemetry not initialised");
    }

    @Test
    void init_idempotent_doesNotThrowOnSecondCall() {
        // Use the test-tracer path to avoid real OTLP export
        ServerTelemetry.setTracerForTesting(
                io.opentelemetry.api.GlobalOpenTelemetry.getTracer("test")
        );
        // A second init() while already initialised must be a no-op
        assertThatCode(() -> ServerTelemetry.setTracerForTesting(
                io.opentelemetry.api.GlobalOpenTelemetry.getTracer("test")
        )).doesNotThrowAnyException();
    }

    @Test
    void shutdown_canBeCalledWithoutInit() {
        assertThatCode(ServerTelemetry::shutdown).doesNotThrowAnyException();
    }

    @Test
    void shutdown_thenTracer_throwsAgain() {
        ServerTelemetry.setTracerForTesting(
                io.opentelemetry.api.GlobalOpenTelemetry.getTracer("test")
        );
        ServerTelemetry.resetForTesting();

        assertThatThrownBy(ServerTelemetry::tracer)
                .isInstanceOf(IllegalStateException.class);
    }
}
