package io.github.roebi.qffxguilib.telemetry;

import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.Tracer;

/**
 * Factory for client-side OTel spans (JavaFX Application Thread measurements).
 *
 * <p>All methods require {@link ClientTelemetry#init(String)} to have been called first.
 *
 * <h2>Measurement points covered</h2>
 * <ol>
 *   <li>{@link #startQueueWait} - time spent in the {@code Platform.runLater()} queue</li>
 *   <li>{@link #fxEventDispatch} - time to execute the FX event on the FAT</li>
 *   <li>{@link #verificationWait} - time and poll count for state assertion</li>
 * </ol>
 *
 * <h2>Typical usage sequence</h2>
 * <pre>
 *   // on calling thread (e.g. QFTest agent callback):
 *   QueueWaitMark mark = ClientSpans.startQueueWait(componentId, stepId);
 *   Platform.runLater(() -> {
 *       mark.arrived();   // ends queue span, records wait_ms
 *       try (ActiveSpan s = ClientSpans.fxEventDispatch(componentId, stepId, "click")) {
 *           node.fireEvent(event);
 *       }
 *   });
 *
 *   // after runLater completes, on calling thread:
 *   try (VerificationSpan vs = ClientSpans.verificationWait(componentId, stepId)) {
 *       while (!condition.isMet()) {
 *           vs.incrementPollCount();
 *           Thread.sleep(pollIntervalMs);
 *       }
 *   }
 * </pre>
 */
public final class ClientSpans {

    private ClientSpans() {}

    // -----------------------------------------------------------------
    // Measurement point 3: Platform.runLater() queue wait
    // -----------------------------------------------------------------

    /**
     * Starts a span immediately before calling {@code Platform.runLater()}.
     * The span ends when {@link QueueWaitMark#arrived()} is called inside the lambda.
     *
     * @param componentId QFTest component identifier
     * @param stepId      QFTest test step ID
     * @return a {@link QueueWaitMark}; call {@link QueueWaitMark#arrived()} on the FAT
     */
    public static QueueWaitMark startQueueWait(String componentId, String stepId) {
        return startQueueWait(ClientTelemetry.tracer(), componentId, stepId);
    }

    /** Overload accepting an explicit tracer. */
    public static QueueWaitMark startQueueWait(
            Tracer tracer,
            String componentId,
            String stepId) {

        Span span = tracer.spanBuilder("fx.queue.wait")
                .setAttribute("qf.component", componentId)
                .setAttribute("qf.step.id",   stepId)
                .startSpan();
        // No scope: span crosses thread boundary, context must not leak
        return new QueueWaitMark(span);
    }

    // -----------------------------------------------------------------
    // Measurement point 4: FX event execution on the FAT
    // -----------------------------------------------------------------

    /**
     * Start a span around the actual {@code node.fireEvent()} call.
     * Must be called on the JavaFX Application Thread.
     *
     * @param componentId QFTest component identifier
     * @param stepId      QFTest test step ID
     * @param eventType   JavaFX event type name, e.g. {@code "MOUSE_CLICKED"}
     * @return an {@link ActiveSpan}; close it after the event fires
     */
    public static ActiveSpan fxEventDispatch(
            String componentId,
            String stepId,
            String eventType) {

        return fxEventDispatch(ClientTelemetry.tracer(), componentId, stepId, eventType);
    }

    /** Overload accepting an explicit tracer. */
    public static ActiveSpan fxEventDispatch(
            Tracer tracer,
            String componentId,
            String stepId,
            String eventType) {

        Span span = tracer.spanBuilder("fx.event.dispatch")
                .setAttribute("qf.component",    componentId)
                .setAttribute("qf.step.id",      stepId)
                .setAttribute("fx.event.type",   eventType)
                .startSpan();
        return new ActiveSpan(span);
    }

    // -----------------------------------------------------------------
    // Measurement point 5: verification / state assertion polling
    // -----------------------------------------------------------------

    /**
     * Start a span for the verification wait loop.
     * The number of poll iterations is recorded automatically when the span closes.
     *
     * @param componentId QFTest component identifier
     * @param stepId      QFTest test step ID
     * @return a {@link VerificationSpan}; call {@link VerificationSpan#incrementPollCount()}
     *         each iteration and close when the condition is met
     */
    public static VerificationSpan verificationWait(
            String componentId,
            String stepId) {

        return verificationWait(ClientTelemetry.tracer(), componentId, stepId);
    }

    /** Overload accepting an explicit tracer. */
    public static VerificationSpan verificationWait(
            Tracer tracer,
            String componentId,
            String stepId) {

        Span span = tracer.spanBuilder("fx.verification.wait")
                .setAttribute("qf.component", componentId)
                .setAttribute("qf.step.id",   stepId)
                .startSpan();
        return new VerificationSpan(span);
    }
}
