package io.github.roebi.qffxguilib.telemetry;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.assertThatCode;

class ClientTelemetryTest {

    @AfterEach
    void tearDown() {
        ClientTelemetry.resetForTesting();
    }

    @Test
    void tracer_throwsIfNotInitialised() {
        assertThatThrownBy(ClientTelemetry::tracer)
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("ClientTelemetry not initialised");
    }

    @Test
    void init_blankClientId_throws() {
        assertThatThrownBy(() -> ClientTelemetry.init("  ", "http://localhost:4318/v1/traces"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("clientId");
    }

    @Test
    void init_nullClientId_throws() {
        assertThatThrownBy(() -> ClientTelemetry.init(null, "http://localhost:4318/v1/traces"))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void shutdown_canBeCalledWithoutInit() {
        assertThatCode(ClientTelemetry::shutdown).doesNotThrowAnyException();
    }

    @Test
    void shutdown_thenTracer_throwsAgain() {
        ClientTelemetry.setTracerForTesting(
                io.opentelemetry.api.GlobalOpenTelemetry.getTracer("test")
        );
        ClientTelemetry.resetForTesting();

        assertThatThrownBy(ClientTelemetry::tracer)
                .isInstanceOf(IllegalStateException.class);
    }
}
