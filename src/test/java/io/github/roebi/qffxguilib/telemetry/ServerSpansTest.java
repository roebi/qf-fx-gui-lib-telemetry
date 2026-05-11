package io.github.roebi.qffxguilib.telemetry;

import io.opentelemetry.api.common.AttributeKey;
import io.opentelemetry.api.trace.Tracer;
import io.opentelemetry.sdk.OpenTelemetrySdk;
import io.opentelemetry.sdk.testing.exporter.InMemorySpanExporter;
import io.opentelemetry.sdk.trace.SdkTracerProvider;
import io.opentelemetry.sdk.trace.export.SimpleSpanProcessor;
import io.opentelemetry.sdk.trace.data.SpanData;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class ServerSpansTest {

    private InMemorySpanExporter exporter;
    private Tracer               tracer;

    @BeforeEach
    void setUp() {
        exporter = InMemorySpanExporter.create();
        SdkTracerProvider provider = SdkTracerProvider.builder()
                .addSpanProcessor(SimpleSpanProcessor.create(exporter))
                .build();
        OpenTelemetrySdk sdk = OpenTelemetrySdk.builder()
                .setTracerProvider(provider)
                .build();
        tracer = sdk.getTracer("test", "1.0.0");
        ServerTelemetry.setTracerForTesting(tracer);
    }

    @AfterEach
    void tearDown() {
        exporter.reset();
        ServerTelemetry.resetForTesting();
    }

    // ------------------------------------------------------------------
    // resolverLookup
    // ------------------------------------------------------------------

    @Test
    void resolverLookup_spanName_isResolverLookup() {
        try (ActiveSpan ignored = ServerSpans.resolverLookup("MyApp_1", "btn.save", "step_42")) {
            // work happens here
        }
        assertThat(finishedSpans().get(0).getName()).isEqualTo("resolver.lookup");
    }

    @Test
    void resolverLookup_hasClientIdAttribute() {
        try (ActiveSpan ignored = ServerSpans.resolverLookup("MyApp_1", "btn.save", "step_42")) {}
        assertAttribute("qf.client.id", "MyApp_1");
    }

    @Test
    void resolverLookup_hasComponentAttribute() {
        try (ActiveSpan ignored = ServerSpans.resolverLookup("MyApp_1", "btn.save", "step_42")) {}
        assertAttribute("qf.component", "btn.save");
    }

    @Test
    void resolverLookup_hasStepIdAttribute() {
        try (ActiveSpan ignored = ServerSpans.resolverLookup("MyApp_1", "btn.save", "step_42")) {}
        assertAttribute("qf.step.id", "step_42");
    }

    @Test
    void resolverLookup_spanIsFinishedAfterClose() {
        try (ActiveSpan ignored = ServerSpans.resolverLookup("MyApp_1", "btn.save", "step_42")) {}
        assertThat(finishedSpans()).hasSize(1);
    }

    @Test
    void resolverLookup_viaExplicitTracer() {
        try (ActiveSpan ignored = ServerSpans.resolverLookup(tracer, "MyApp_2", "lbl.title", "step_7")) {}
        assertThat(finishedSpans()).hasSize(1);
        assertAttribute("qf.client.id", "MyApp_2");
    }

    // ------------------------------------------------------------------
    // agentRoundTrip
    // ------------------------------------------------------------------

    @Test
    void agentRoundTrip_spanName_isAgentCommandRoundtrip() {
        try (ActiveSpan ignored = ServerSpans.agentRoundTrip("MyApp_1", "click")) {}
        assertThat(finishedSpans().get(0).getName()).isEqualTo("agent.command.roundtrip");
    }

    @Test
    void agentRoundTrip_hasClientIdAttribute() {
        try (ActiveSpan ignored = ServerSpans.agentRoundTrip("MyApp_1", "click")) {}
        assertAttribute("qf.client.id", "MyApp_1");
    }

    @Test
    void agentRoundTrip_hasCommandTypeAttribute() {
        try (ActiveSpan ignored = ServerSpans.agentRoundTrip("MyApp_1", "setText")) {}
        assertAttribute("qf.command.type", "setText");
    }

    @Test
    void agentRoundTrip_viaExplicitTracer() {
        try (ActiveSpan ignored = ServerSpans.agentRoundTrip(tracer, "MyApp_2", "check")) {}
        assertThat(finishedSpans()).hasSize(1);
    }

    // ------------------------------------------------------------------
    // ActiveSpan extras
    // ------------------------------------------------------------------

    @Test
    void activeSpan_setLongAttribute_isRecorded() {
        try (ActiveSpan s = ServerSpans.resolverLookup("c", "comp", "step")) {
            s.setAttribute("custom.count", 42L);
        }
        SpanData data = finishedSpans().get(0);
        assertThat(data.getAttributes().get(AttributeKey.longKey("custom.count")))
                .isEqualTo(42L);
    }

    @Test
    void activeSpan_recordException_marksSpanAsError() {
        try (ActiveSpan s = ServerSpans.resolverLookup("c", "comp", "step")) {
            s.recordException(new RuntimeException("test error"));
        }
        SpanData data = finishedSpans().get(0);
        assertThat(data.getStatus().getStatusCode())
                .isEqualTo(io.opentelemetry.api.trace.StatusCode.ERROR);
    }

    // ------------------------------------------------------------------
    // helpers
    // ------------------------------------------------------------------

    private List<SpanData> finishedSpans() {
        return exporter.getFinishedSpanItems();
    }

    private void assertAttribute(String key, String expectedValue) {
        SpanData data = finishedSpans().get(0);
        assertThat(data.getAttributes().get(AttributeKey.stringKey(key)))
                .as("attribute '%s'", key)
                .isEqualTo(expectedValue);
    }
}
