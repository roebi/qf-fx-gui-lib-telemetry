package io.github.roebi.qffxguilib.telemetry;

import io.opentelemetry.api.common.AttributeKey;
import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.api.trace.Tracer;
import io.opentelemetry.exporter.otlp.http.trace.OtlpHttpSpanExporter;
import io.opentelemetry.sdk.OpenTelemetrySdk;
import io.opentelemetry.sdk.resources.Resource;
import io.opentelemetry.sdk.trace.SdkTracerProvider;
import io.opentelemetry.sdk.trace.export.BatchSpanProcessor;

/**
 * Bootstrap for a QFTest <em>client</em> JVM (one per SUT instance).
 *
 * <p>Call {@link #init(String)} once in your client-side library init hook,
 * passing the QFTest client ID so all spans carry it as a resource attribute.
 * Call {@link #shutdown()} when the client is stopped.
 *
 * <p>This class is thread-safe. Repeated calls to {@link #init(String)} with
 * the same client ID are idempotent.
 */
public final class ClientTelemetry {

    public static final String DEFAULT_ENDPOINT =
            "http://localhost:4318/v1/traces";

    static final String INSTRUMENTATION_NAME    = "qf-fx-gui-lib.client";
    static final String INSTRUMENTATION_VERSION = "1.0.0";

    private static volatile OpenTelemetrySdk sdk;
    private static volatile Tracer tracer;

    private ClientTelemetry() {}

    /**
     * Initialise with the QFTest client ID and the default local endpoint.
     *
     * @param clientId QFTest client identifier, e.g. {@code "MyApp_1"}
     */
    public static synchronized void init(String clientId) {
        init(clientId, DEFAULT_ENDPOINT);
    }

    /**
     * Initialise with the QFTest client ID and a custom OTLP/HTTP endpoint.
     *
     * @param clientId        QFTest client identifier, e.g. {@code "MyApp_1"}
     * @param otlpHttpEndpoint full URL, e.g. {@code http://my-collector:4318/v1/traces}
     */
    public static synchronized void init(String clientId, String otlpHttpEndpoint) {
        if (sdk != null) {
            return; // idempotent
        }
        if (clientId == null || clientId.isBlank()) {
            throw new IllegalArgumentException("clientId must not be blank");
        }

        Resource resource = Resource.getDefault().merge(
                Resource.create(Attributes.of(
                        AttributeKey.stringKey("service.name"), "qftest-client",
                        AttributeKey.stringKey("qf.client.id"), clientId
                ))
        );

        SdkTracerProvider provider = SdkTracerProvider.builder()
                .setResource(resource)
                .addSpanProcessor(BatchSpanProcessor.builder(
                        OtlpHttpSpanExporter.builder()
                                .setEndpoint(otlpHttpEndpoint)
                                .build()
                ).build())
                .build();

        sdk = OpenTelemetrySdk.builder()
                .setTracerProvider(provider)
                .build();

        tracer = sdk.getTracer(INSTRUMENTATION_NAME, INSTRUMENTATION_VERSION);
    }

    /**
     * Flush buffered spans and release resources.
     * Call when the client JVM is stopped.
     */
    public static synchronized void shutdown() {
        if (sdk != null) {
            sdk.close();
            sdk    = null;
            tracer = null;
        }
    }

    /**
     * Returns the tracer. Throws if {@link #init(String)} has not been called.
     * Package-private: used by {@link ClientSpans} and tests.
     */
    static Tracer tracer() {
        Tracer t = tracer;
        if (t == null) {
            throw new IllegalStateException(
                    "ClientTelemetry not initialised - call ClientTelemetry.init(clientId) first");
        }
        return t;
    }

    /** Package-private: for use in unit tests only. */
    static synchronized void setTracerForTesting(Tracer t) {
        tracer = t;
    }

    /** Package-private: for use in unit tests only. */
    static synchronized void resetForTesting() {
        if (sdk != null) {
            sdk.close();
            sdk = null;
        }
        tracer = null;
    }
}
