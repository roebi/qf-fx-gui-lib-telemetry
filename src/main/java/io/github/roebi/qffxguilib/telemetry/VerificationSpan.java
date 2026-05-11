package io.github.roebi.qffxguilib.telemetry;

import io.opentelemetry.api.trace.Span;
import io.opentelemetry.context.Scope;

/**
 * AutoCloseable span for the verification / polling wait phase.
 * Tracks how many poll iterations were needed to satisfy the state condition.
 *
 * <p>Usage:
 * <pre>
 *   try (VerificationSpan vs = ClientSpans.verificationWait(componentId, stepId)) {
 *       while (!stateCondition.isMet()) {
 *           vs.incrementPollCount();
 *           Thread.sleep(pollIntervalMs);
 *       }
 *   }
 *   // span ends here, fx.verification.polls is recorded
 * </pre>
 */
public final class VerificationSpan implements AutoCloseable {

    private final Span  span;
    private final Scope scope;
    private int pollCount = 0;

    VerificationSpan(Span span) {
        this.span  = span;
        this.scope = span.makeCurrent();
    }

    /**
     * Call once per polling iteration inside your wait loop.
     * The total is recorded as {@code fx.verification.polls} when the span closes.
     */
    public void incrementPollCount() {
        pollCount++;
    }

    /** Current poll count. Exposed for testing and conditional logging. */
    public int pollCount() {
        return pollCount;
    }

    @Override
    public void close() {
        span.setAttribute("fx.verification.polls", pollCount);
        try {
            scope.close();
        } finally {
            span.end();
        }
    }
}
