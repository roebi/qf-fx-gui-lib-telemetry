package io.github.roebi.qffxguilib.telemetry;

import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.StatusCode;
import io.opentelemetry.context.Scope;

/**
 * AutoCloseable wrapper around an active OTel Span + Scope.
 * Use with try-with-resources:
 * <pre>
 *   try (ActiveSpan s = ServerSpans.resolverLookup(clientId, componentId, stepId)) {
 *       return resolver.resolve(componentId);
 *   }
 * </pre>
 * Closing this object ends the scope and the span in the correct order.
 */
public final class ActiveSpan implements AutoCloseable {

    private final Span span;
    private final Scope scope;

    ActiveSpan(Span span) {
        this.span  = span;
        this.scope = span.makeCurrent();
    }

    /** Set an additional long attribute before the span ends. */
    public ActiveSpan setAttribute(String key, long value) {
        span.setAttribute(key, value);
        return this;
    }

    /** Set an additional String attribute before the span ends. */
    public ActiveSpan setAttribute(String key, String value) {
        span.setAttribute(key, value);
        return this;
    }

    /**
     * Record an exception and mark the span as ERROR.
     * Typically called from a catch block before re-throwing.
     */
    public ActiveSpan recordException(Throwable t) {
        span.recordException(t);
        span.setStatus(StatusCode.ERROR, t.getMessage());
        return this;
    }

    /** Access the underlying OTel Span for advanced use. */
    public Span span() {
        return span;
    }

    @Override
    public void close() {
        try {
            scope.close();
        } finally {
            span.end();
        }
    }
}
