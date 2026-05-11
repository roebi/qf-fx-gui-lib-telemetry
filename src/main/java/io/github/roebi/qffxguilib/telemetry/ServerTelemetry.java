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
 * Bootstrap for the QFTest <em>server</em> JVM.
 *
 * <p>Call {@link #init()} once during suite initialisation,
 * typically at the end of your resolver-registration hook.
 * Call {@link #shutdown()} in suite teardown to flush pending spans.
 *
 * <p>This class is thread-safe. Repeated calls to {@link #init()} are idempotent.
 */
public final class ServerTelemetry {

    public static final String DEFAULT_ENDPOINT =
            "http://localhost:4318/v1/traces";

    static final String INSTRUMENTATION_NAME    = "qf-fx-gui-lib.server";
    static final String INSTRUMENTATION_VERSION = "1.0.0";

    private static volatile OpenTelemetrySdk sdk;
    private static volatile Tracer tracer;

    private ServerTelemetry() {}

    /** Initialise with the default local Jaeger OTLP/HTTP endpoint. */
    public static synchronized void init() {
        init(DEFAULT_ENDPOINT);
    }

    /**
     * Initialise with a custom OTLP/HTTP endpoint.
     *
     * @param otlpHttpEndpoint full URL including path,
     *                         e.g. {@code http://my-collector:4318/v1/traces}
     */
    public static synchronized void init(String otlpHttpEndpoint) {
        if (sdk != null) {
            return; // idempotent
        }
        Resource resource = Resource.getDefault().merge(
                Resource.create(Attributes.of(
                        AttributeKey.stringKey("service.name"), "qftest-server"
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
     * Call in suite teardown.
     */
    public static synchronized void shutdown() {
        if (sdk != null) {
            sdk.close();
            sdk    = null;
            tracer = null;
        }
    }

    /**
     * Returns the tracer. Throws if {@link #init()} has not been called.
     * Package-private: used by {@link ServerSpans} and tests.
     */
    static Tracer tracer() {
        Tracer t = tracer;
        if (t == null) {
            throw new IllegalStateException(
                    "ServerTelemetry not initialised - call ServerTelemetry.init() first");
        }
        return t;
    }

    /**
     * Injects a test tracer, bypassing OTLP export.
     * Package-private: for use in unit tests only.
     */
    static synchronized void setTracerForTesting(Tracer t) {
        tracer = t;
    }

    /**
     * Resets all state. Package-private: for use in unit tests only.
     */
    static synchronized void resetForTesting() {
        if (sdk != null) {
            sdk.close();
            sdk = null;
        }
        tracer = null;
    }
}
