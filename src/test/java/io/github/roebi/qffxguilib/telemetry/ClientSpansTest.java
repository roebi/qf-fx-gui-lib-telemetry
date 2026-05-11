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

class ClientSpansTest {

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
        ClientTelemetry.setTracerForTesting(tracer);
    }

    @AfterEach
    void tearDown() {
        exporter.reset();
        ClientTelemetry.resetForTesting();
    }

    // ------------------------------------------------------------------
    // startQueueWait / QueueWaitMark
    // ------------------------------------------------------------------

    @Test
    void queueWait_spanName_isFxQueueWait() {
        QueueWaitMark mark = ClientSpans.startQueueWait("btn.ok", "step_1");
        mark.arrived();
        assertThat(finishedSpans().get(0).getName()).isEqualTo("fx.queue.wait");
    }

    @Test
    void queueWait_hasComponentAttribute() {
        QueueWaitMark mark = ClientSpans.startQueueWait("btn.ok", "step_1");
        mark.arrived();
        assertAttribute("qf.component", "btn.ok");
    }

    @Test
    void queueWait_hasStepIdAttribute() {
        QueueWaitMark mark = ClientSpans.startQueueWait("btn.ok", "step_1");
        mark.arrived();
        assertAttribute("qf.step.id", "step_1");
    }

    @Test
    void queueWait_recordsWaitMsAttribute() {
        QueueWaitMark mark = ClientSpans.startQueueWait("btn.ok", "step_1");
        mark.arrived();
        SpanData data = finishedSpans().get(0);
        Long waitMs = data.getAttributes().get(AttributeKey.longKey("fx.queue.wait_ms"));
        assertThat(waitMs).isNotNull().isGreaterThanOrEqualTo(0L);
    }

    @Test
    void queueWait_spanIsFinishedAfterArrived() {
        QueueWaitMark mark = ClientSpans.startQueueWait("comp", "step");
        assertThat(finishedSpans()).isEmpty(); // not finished yet
        mark.arrived();
        assertThat(finishedSpans()).hasSize(1);
    }

    @Test
    void queueWait_enqueueNanos_capturedAtCreation() {
        long before = System.nanoTime();
        QueueWaitMark mark = ClientSpans.startQueueWait("comp", "step");
        long after  = System.nanoTime();
        mark.arrived();
        assertThat(mark.enqueueNanos()).isBetween(before, after);
    }

    @Test
    void queueWait_viaExplicitTracer() {
        QueueWaitMark mark = ClientSpans.startQueueWait(tracer, "lbl", "step");
        mark.arrived();
        assertThat(finishedSpans()).hasSize(1);
    }

    // ------------------------------------------------------------------
    // fxEventDispatch
    // ------------------------------------------------------------------

    @Test
    void fxEventDispatch_spanName_isFxEventDispatch() {
        try (ActiveSpan ignored = ClientSpans.fxEventDispatch("btn.ok", "step_2", "MOUSE_CLICKED")) {}
        assertThat(finishedSpans().get(0).getName()).isEqualTo("fx.event.dispatch");
    }

    @Test
    void fxEventDispatch_hasComponentAttribute() {
        try (ActiveSpan ignored = ClientSpans.fxEventDispatch("btn.ok", "step_2", "MOUSE_CLICKED")) {}
        assertAttribute("qf.component", "btn.ok");
    }

    @Test
    void fxEventDispatch_hasStepIdAttribute() {
        try (ActiveSpan ignored = ClientSpans.fxEventDispatch("btn.ok", "step_2", "MOUSE_CLICKED")) {}
        assertAttribute("qf.step.id", "step_2");
    }

    @Test
    void fxEventDispatch_hasEventTypeAttribute() {
        try (ActiveSpan ignored = ClientSpans.fxEventDispatch("btn.ok", "step_2", "MOUSE_CLICKED")) {}
        assertAttribute("fx.event.type", "MOUSE_CLICKED");
    }

    @Test
    void fxEventDispatch_viaExplicitTracer() {
        try (ActiveSpan ignored = ClientSpans.fxEventDispatch(tracer, "tf.name", "step_3", "KEY_PRESSED")) {}
        assertThat(finishedSpans()).hasSize(1);
    }

    // ------------------------------------------------------------------
    // verificationWait / VerificationSpan
    // ------------------------------------------------------------------

    @Test
    void verificationWait_spanName_isFxVerificationWait() {
        try (VerificationSpan ignored = ClientSpans.verificationWait("btn.ok", "step_3")) {}
        assertThat(finishedSpans().get(0).getName()).isEqualTo("fx.verification.wait");
    }

    @Test
    void verificationWait_hasComponentAttribute() {
        try (VerificationSpan ignored = ClientSpans.verificationWait("btn.ok", "step_3")) {}
        assertAttribute("qf.component", "btn.ok");
    }

    @Test
    void verificationWait_hasStepIdAttribute() {
        try (VerificationSpan ignored = ClientSpans.verificationWait("btn.ok", "step_3")) {}
        assertAttribute("qf.step.id", "step_3");
    }

    @Test
    void verificationWait_pollCountZeroIfNeverIncremented() {
        try (VerificationSpan ignored = ClientSpans.verificationWait("comp", "step")) {}
        SpanData data = finishedSpans().get(0);
        Long polls = data.getAttributes().get(AttributeKey.longKey("fx.verification.polls"));
        assertThat(polls).isEqualTo(0L);
    }

    @Test
    void verificationWait_pollCountMatchesIncrementCalls() {
        try (VerificationSpan vs = ClientSpans.verificationWait("comp", "step")) {
            vs.incrementPollCount();
            vs.incrementPollCount();
            vs.incrementPollCount();
            assertThat(vs.pollCount()).isEqualTo(3);
        }
        SpanData data = finishedSpans().get(0);
        Long polls = data.getAttributes().get(AttributeKey.longKey("fx.verification.polls"));
        assertThat(polls).isEqualTo(3L);
    }

    @Test
    void verificationWait_spanNotFinishedBeforeClose() {
        VerificationSpan vs = ClientSpans.verificationWait("comp", "step");
        assertThat(finishedSpans()).isEmpty();
        vs.close();
        assertThat(finishedSpans()).hasSize(1);
    }

    @Test
    void verificationWait_viaExplicitTracer() {
        try (VerificationSpan ignored = ClientSpans.verificationWait(tracer, "lbl", "step")) {}
        assertThat(finishedSpans()).hasSize(1);
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
