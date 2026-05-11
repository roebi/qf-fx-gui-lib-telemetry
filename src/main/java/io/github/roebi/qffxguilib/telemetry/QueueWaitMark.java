package io.github.roebi.qffxguilib.telemetry;

import io.opentelemetry.api.trace.Span;

/**
 * Measures the time a {@code Platform.runLater()} call waits in the JavaFX
 * Application Thread (FAT) queue before execution begins.
 *
 * <p>The span is started on the <em>enqueuing</em> thread and ended on the FAT.
 * No OTel {@link io.opentelemetry.context.Scope} is held because context must
 * not leak across threads.
 *
 * <p>Usage:
 * <pre>
 *   QueueWaitMark mark = ClientSpans.startQueueWait(componentId, stepId);
 *   Platform.runLater(() -> {
 *       mark.arrived();          // records wait_ms, ends the span
 *       dispatchFxEvent(...);
 *   });
 * </pre>
 */
public final class QueueWaitMark {

    private final Span span;
    private final long enqueueNanos;

    QueueWaitMark(Span span) {
        this.span         = span;
        this.enqueueNanos = System.nanoTime();
    }

    /**
     * Must be called as the <em>first</em> statement inside the
     * {@code Platform.runLater()} lambda body, on the FAT.
     * Records {@code fx.queue.wait_ms} and ends the span.
     */
    public void arrived() {
        long waitMs = (System.nanoTime() - enqueueNanos) / 1_000_000L;
        span.setAttribute("fx.queue.wait_ms", waitMs);
        span.end();
    }

    /** Nanosecond timestamp captured at enqueue time. Exposed for testing. */
    long enqueueNanos() {
        return enqueueNanos;
    }
}
