package pt.ulisboa.tecnico.socialsoftware.ms.utils;

import io.opentelemetry.api.GlobalOpenTelemetry;
import io.opentelemetry.api.common.AttributeKey;
import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.SpanContext;
import io.opentelemetry.api.trace.SpanKind;
import io.opentelemetry.api.trace.StatusCode;
import io.opentelemetry.api.trace.TraceFlags;
import io.opentelemetry.api.trace.TraceState;
import io.opentelemetry.api.trace.Tracer;
import io.opentelemetry.context.Context;
import io.opentelemetry.exporter.otlp.trace.OtlpGrpcSpanExporter;
import io.opentelemetry.sdk.OpenTelemetrySdk;
import io.opentelemetry.sdk.resources.Resource;
import io.opentelemetry.sdk.trace.SdkTracerProvider;
import io.opentelemetry.sdk.trace.export.BatchSpanProcessor;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

public class TraceManager {

    // --- Static Fields ---
    private static volatile TraceManager INSTANCE;
    private static Span masterRootSpan;
    private static Span rootSpan;
    private static int rootSpanId = 1;

    // --- Instance Fields ---
    private final String serviceName;
    private final Tracer tracer;
    private final SdkTracerProvider tracerProvider;
    private final Tracer masterRootTracer;
    private final SdkTracerProvider masterRootTracerProvider;
    private final Map<String, Span> functionalitySpans = new ConcurrentHashMap<>();
    private final Map<String, Span> stepSpans = new ConcurrentHashMap<>();
    private final Map<String, AtomicInteger> functionalityCounters = new ConcurrentHashMap<>();
    private final Map<String, java.util.concurrent.ConcurrentLinkedQueue<String>> activeInvocationKeys = new ConcurrentHashMap<>();

    // --- Constructor ---
    private TraceManager(String serviceName) {
        this.serviceName = serviceName;

        String otlpEndpoint = Objects.requireNonNull(
                System.getenv().getOrDefault("OTEL_EXPORTER_OTLP_ENDPOINT", "http://localhost:4317"));

        OtlpGrpcSpanExporter spanExporter = OtlpGrpcSpanExporter.builder()
                .setEndpoint(otlpEndpoint)
                .build();

        Resource serviceResource = Objects.requireNonNull(Resource.getDefault().merge(
                Resource.create(
                        Attributes.of(AttributeKey.stringKey("service.name"), serviceName))));

        SdkTracerProvider tracerProvider = SdkTracerProvider.builder()
                .setResource(serviceResource)
                .addSpanProcessor(BatchSpanProcessor.builder(spanExporter).build())
                .build();

        this.tracerProvider = tracerProvider;

        OpenTelemetrySdk.builder()
                .setTracerProvider(tracerProvider)
                .buildAndRegisterGlobal();

        this.tracer = GlobalOpenTelemetry.getTracer(serviceName);

        // Separate TracerProvider for master root span with service.name = "root"
        OtlpGrpcSpanExporter masterExporter = OtlpGrpcSpanExporter.builder()
                .setEndpoint(otlpEndpoint)
                .build();

        Resource masterResource = Objects.requireNonNull(Resource.getDefault().merge(
                Resource.create(
                        Attributes.of(AttributeKey.stringKey("service.name"), "root"))));

        this.masterRootTracerProvider = SdkTracerProvider.builder()
                .setResource(masterResource)
                .addSpanProcessor(BatchSpanProcessor.builder(masterExporter).build())
                .build();

        this.masterRootTracer = masterRootTracerProvider.get("root");
    }

    // --- Singleton Access ---
    public static synchronized void init(String serviceName) {
        if (INSTANCE == null) {
            INSTANCE = new TraceManager(serviceName);
        }
    }

    public static TraceManager getInstance() {
        return INSTANCE;
    }

    // --- Master Root Span ---
    public void createMasterRoot() {
        String rootSpanName = "root" + rootSpanId;
        masterRootSpan = masterRootTracer.spanBuilder(rootSpanName)
                .setSpanKind(SpanKind.INTERNAL)
                .startSpan();
    }

    public String getMasterRootTraceId() {
        return masterRootSpan != null ? masterRootSpan.getSpanContext().getTraceId() : null;
    }

    public String getMasterRootSpanId() {
        return masterRootSpan != null ? masterRootSpan.getSpanContext().getSpanId() : null;
    }

    // --- Root Span Management ---
    public void startRootSpan() {
        String rootSpanName = serviceName + "-root" + rootSpanId;
        Span span = tracer.spanBuilder(rootSpanName)
                .setSpanKind(SpanKind.INTERNAL)
                .startSpan();
        rootSpan = span;
        span.setAttribute("root", "root");
        span.setAttribute("service", serviceName);
        rootSpanId++;
    }

    public void startRootSpan(String traceId, String spanId) {
        SpanContext parentContext = SpanContext.createFromRemoteParent(
                traceId, spanId, TraceFlags.getSampled(), TraceState.getDefault());
        Context remoteContext = Context.current().with(Span.wrap(parentContext));
        String rootSpanName = serviceName + "-root" + rootSpanId;
        Span span = tracer.spanBuilder(rootSpanName)
                .setParent(remoteContext)
                .setSpanKind(SpanKind.INTERNAL)
                .startSpan();
        rootSpan = span;
        span.setAttribute("root", "root");
        span.setAttribute("service", serviceName);
        rootSpanId++;
    }

    public String getRootTraceId() {
        return rootSpan != null ? rootSpan.getSpanContext().getTraceId() : null;
    }

    public String getRootSpanId() {
        return rootSpan != null ? rootSpan.getSpanContext().getSpanId() : null;
    }

    public void endRootSpan() {
        if (rootSpan != null) {
            rootSpan.end();
            rootSpan = null;
            activeInvocationKeys.clear();
            functionalityCounters.clear();
            functionalitySpans.clear();
            stepSpans.clear();
        } else {
            throw new IllegalStateException("Root span has not been started");
        }
    }

    public void endMasterRootSpan() {
        if (masterRootSpan != null) {
            masterRootSpan.end();
            masterRootSpan = null;
        }
    }

    // --- Functionality Span Management ---
    public String startSpanForFunctionality(String func) {
        if (rootSpan == null) {
            return null;
        }
        int counter = functionalityCounters.computeIfAbsent(func, k -> new AtomicInteger(0)).getAndIncrement();
        String invocationKey = func + "::" + counter;
        Span span = tracer.spanBuilder(invocationKey)
                .setParent(Context.current().with(rootSpan))
                .setSpanKind(SpanKind.INTERNAL)
                .startSpan();

        span.setAttribute("func.name", invocationKey);
        span.setAttribute("functionality", func);

        functionalitySpans.put(invocationKey, span);
        activeInvocationKeys.computeIfAbsent(func, k -> new java.util.concurrent.ConcurrentLinkedQueue<>())
                .add(invocationKey);

        return invocationKey;
    }

    public void endSpanForFunctionality(String func) {
        java.util.concurrent.ConcurrentLinkedQueue<String> queue = activeInvocationKeys.get(func);
        if (queue == null || queue.isEmpty())
            return;
        String invocationKey = queue.poll();
        if (invocationKey == null)
            return;

        forceEndAllActiveStepsSpans(invocationKey);

        functionalitySpans.computeIfPresent(invocationKey, (f, span) -> {
            span.end();
            return null;
        });
    }

    public Span getSpanForFunctionality(String func, int counter) {
        return functionalitySpans.get(func + "::" + counter);
    }

    public Span getSpanForFunctionality(String func) {
        java.util.concurrent.ConcurrentLinkedQueue<String> queue = activeInvocationKeys.get(func);
        if (queue == null || queue.isEmpty())
            return null;
        String latestKey = queue.peek();
        return functionalitySpans.get(latestKey);
    }

    // --- Compensation Span Management ---
    public void startSpanForCompensation(String func) {
        if (rootSpan == null) {
            return;
        }
        int counter = functionalityCounters.computeIfAbsent("[Compensate]" + func, k -> new AtomicInteger(0))
                .getAndIncrement();
        String invocationKey = "[Compensate]" + func + "::" + counter;
        Span span = tracer.spanBuilder(invocationKey)
                .setParent(Context.current().with(rootSpan))
                .setSpanKind(SpanKind.INTERNAL)
                .startSpan();

        span.setAttribute("func.name", invocationKey);
        span.setAttribute("functionality", func);

        functionalitySpans.put(invocationKey, span);
        activeInvocationKeys
                .computeIfAbsent("[Compensate]" + func, k -> new java.util.concurrent.ConcurrentLinkedQueue<>())
                .add(invocationKey);
    }

    public void endSpanForCompensation(String func) {
        java.util.concurrent.ConcurrentLinkedQueue<String> queue = activeInvocationKeys.get("[Compensate]" + func);
        if (queue == null || queue.isEmpty())
            return;
        String invocationKey = queue.poll();
        if (invocationKey == null)
            return;

        forceEndAllActiveStepsSpans(invocationKey);

        functionalitySpans.computeIfPresent(invocationKey, (f, span) -> {
            span.end();
            return null;
        });
    }

    // --- Step Span Management ---
    public Span getStepSpan(String func, String stepName) {
        java.util.concurrent.ConcurrentLinkedQueue<String> queue = activeInvocationKeys.get(func);
        if (queue == null || queue.isEmpty()) {
            return null;
        }
        String firstInvocationKey = queue.peek();
        String key = key(firstInvocationKey, stepName);
        return stepSpans.get(key);
    }

    public void startStepSpan(String func, String stepName) {
        java.util.concurrent.ConcurrentLinkedQueue<String> queue = activeInvocationKeys.get(func);
        if (queue == null || queue.isEmpty()) {
            return;
        }
        String firstInvocationKey = queue.peek();
        Span parentSpan = functionalitySpans.get(firstInvocationKey);

        if (parentSpan == null) {
            return;
        }

        Span stepSpan = tracer.spanBuilder(stepName)
                .setParent(Context.current().with(parentSpan))
                .setSpanKind(SpanKind.INTERNAL)
                .startSpan();

        stepSpan.setAttribute("step.name", stepName);
        stepSpan.setAttribute("functionality", func);

        stepSpans.put(key(firstInvocationKey, stepName), stepSpan);
    }

    public void endStepSpan(String func, String stepName) {
        java.util.concurrent.ConcurrentLinkedQueue<String> queue = activeInvocationKeys.get(func);
        if (queue == null || queue.isEmpty()) {
            return;
        }
        String firstInvocationKey = queue.peek();
        String key = key(firstInvocationKey, stepName);
        Span stepSpan = stepSpans.get(key);
        if (stepSpan != null) {
            stepSpan.end();
        }
    }

    // --- Delay Span Management ---
    public Span startDelaySpan(String func, String step, int delay, boolean isBefore) {
        if (delay <= 0) {
            return null;
        }
        Span parentSpan = getStepSpan(func, step);
        if (parentSpan == null)
            return null;
        String spanName = (isBefore ? "before" : "after");
        Span span = tracer.spanBuilder(spanName)
                .setParent(Context.current().with(parentSpan))
                .setSpanKind(SpanKind.INTERNAL)
                .startSpan();

        span.setAttribute("functionality", func);
        span.setAttribute("step", step);
        span.setAttribute("value", Integer.toString(delay) + " ms");

        return span;
    }

    public void endDelaySpan(Span delaySpan) {
        if (delaySpan != null) {
            delaySpan.end();
        }
    }

    // --- Utility Methods ---
    public void forceFlush() {
        try {
            tracerProvider.forceFlush().join(2, TimeUnit.SECONDS);
            masterRootTracerProvider.forceFlush().join(2, TimeUnit.SECONDS);
        } catch (Exception e) {
            return;
        }
    }

    public void recordException(String func, Throwable e, String message) {
        Span span = getSpanForFunctionality(func);
        if (span == null)
            return;
        span.recordException(e);
        span.setStatus(StatusCode.ERROR, message != null ? message : e.getMessage());
        span.addEvent("exception.caught", Attributes.of(
                AttributeKey.stringKey("exception.type"), e.getClass().getSimpleName(),
                AttributeKey.stringKey("exception.message"), e.getMessage()));
    }

    public void recordWarning(String func, Exception e, String message) {
        Span span = getSpanForFunctionality(func);
        if (span == null)
            return;
        span.addEvent("warning", Attributes.of(
                AttributeKey.stringKey("exception.type"), e.getClass().getSimpleName(),
                AttributeKey.stringKey("message"), message));
    }

    public void setSpanAttribute(String func, String key, String value) {
        Span span = getSpanForFunctionality(func);
        if (span != null) {
            span.setAttribute(key, value);
        }
    }

    // --- Private Helpers ---
    private void forceEndAllActiveStepsSpans(String invocationKey) {
        String prefix = invocationKey + "::";
        List<String> stepSpansList = stepSpans.keySet().stream()
                .filter(key -> key.startsWith(prefix))
                .collect(Collectors.toList());
        for (String key : stepSpansList) {
            String stepName = key.substring(prefix.length());
            Span stepSpan = stepSpans.get(key);
            if (stepSpan != null) {
                stepSpan.setAttribute("Warning", "Forced end step span");
                stepSpan.addEvent("forced-end", Attributes.of(
                        AttributeKey.stringKey("reason"), "Functionality was aborted!"));
                stepSpan.end();
                stepSpans.remove(key);
            }
        }
    }

    private String key(String func, String stepName) {
        return func + "::" + stepName;
    }
}
