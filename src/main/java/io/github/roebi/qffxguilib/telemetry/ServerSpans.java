package io.github.roebi.qffxguilib.telemetry;

import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.Tracer;

/**
 * Factory for server-side OTel spans.
 *
 * <p>All methods require {@link ServerTelemetry#init()} to have been called first.
 * Use with try-with-resources:
 * <pre>
 *   try (ActiveSpan s = ServerSpans.resolverLookup(clientId, componentId, stepId)) {
 *       return resolverRegistry.resolve(componentId);
 *   }
 * </pre>
 *
 * <h2>Measurement points covered</h2>
 * <ol>
 *   <li>{@link #resolverLookup} - time to find the resolver for a component</li>
 *   <li>{@link #agentRoundTrip} - time from command send to client ACK</li>
 * </ol>
 */
public final class ServerSpans {

    private ServerSpans() {}

    // -----------------------------------------------------------------
    // Measurement point 1: resolver lookup
    // -----------------------------------------------------------------

    /**
     * Start a span for the resolver lookup phase.
     *
     * @param clientId    QFTest client ID, e.g. {@code "MyApp_1"}
     * @param componentId QFTest component identifier
     * @param stepId      QFTest test step ID for cross-JVM correlation
     * @return an {@link ActiveSpan}; close it to end the span
     */
    public static ActiveSpan resolverLookup(
            String clientId,
            String componentId,
            String stepId) {

        return resolverLookup(ServerTelemetry.tracer(), clientId, componentId, stepId);
    }

    /** Overload accepting an explicit tracer; useful for tests and custom setups. */
    public static ActiveSpan resolverLookup(
            Tracer tracer,
            String clientId,
            String componentId,
            String stepId) {

        Span span = tracer.spanBuilder("resolver.lookup")
                .setAttribute("qf.client.id",  clientId)
                .setAttribute("qf.component",   componentId)
                .setAttribute("qf.step.id",     stepId)
                .startSpan();
        return new ActiveSpan(span);
    }

    // -----------------------------------------------------------------
    // Measurement point 2: agent protocol round-trip
    // -----------------------------------------------------------------

    /**
     * Start a span that wraps the agent protocol send-and-await-ack call.
     *
     * @param clientId    QFTest client ID
     * @param commandType short label for the command type, e.g. {@code "click"}
     * @return an {@link ActiveSpan}; close it when the ack is received
     */
    public static ActiveSpan agentRoundTrip(
            String clientId,
            String commandType) {

        return agentRoundTrip(ServerTelemetry.tracer(), clientId, commandType);
    }

    /** Overload accepting an explicit tracer. */
    public static ActiveSpan agentRoundTrip(
            Tracer tracer,
            String clientId,
            String commandType) {

        Span span = tracer.spanBuilder("agent.command.roundtrip")
                .setAttribute("qf.client.id",    clientId)
                .setAttribute("qf.command.type",  commandType)
                .startSpan();
        return new ActiveSpan(span);
    }
}
