package pt.ulisboa.tecnico.socialsoftware.ms.monitoring;

import io.opentelemetry.api.GlobalOpenTelemetry;
import io.opentelemetry.api.common.AttributeKey;
import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.api.trace.*;
import io.opentelemetry.context.Context;
import io.opentelemetry.exporter.otlp.trace.OtlpGrpcSpanExporter;
import io.opentelemetry.sdk.OpenTelemetrySdk;
import io.opentelemetry.sdk.resources.Resource;
import io.opentelemetry.sdk.trace.SdkTracerProvider;
import io.opentelemetry.sdk.trace.export.BatchSpanProcessor;
import pt.ulisboa.tecnico.socialsoftware.ms.messaging.Command;
import pt.ulisboa.tecnico.socialsoftware.ms.transaction.unitOfWork.UnitOfWork;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.WeakHashMap;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
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
    private final Map<String, Span> commandSpans = new ConcurrentHashMap<>();
    private final Map<String, Long> queueWaitTimesNano = new ConcurrentHashMap<>();
    private final Map<String, Integer> commandRetryCounters = new ConcurrentHashMap<>();
    private final Map<UnitOfWork, String> uowTraceIds = Collections.synchronizedMap(new WeakHashMap<>());

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
            functionalitySpans.clear();
            commandSpans.clear();
            commandRetryCounters.clear();
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
    public void startSpanForFunctionality(String executionId, String func) {
        if (rootSpan == null) {
            return;
        }
        String invocationKey = func + "::" + executionId;
        Span span = tracer.spanBuilder(invocationKey)
                .setParent(Context.current().with(rootSpan))
                .setSpanKind(SpanKind.INTERNAL)
                .startSpan();

        span.setAttribute("functionality", func);
        span.setAttribute("executionId", executionId);
        span.setAttribute("isImpaired", false); // defaut behaviour is not being impaired

        functionalitySpans.put(executionId, span);
    }

    public void endSpanForFunctionality(String executionId, UnitOfWork unitOfWork) {
        Span span = functionalitySpans.remove(executionId);
        if (span == null) {
            return;
        }

        forceEndAllActiveCommandsSpans(executionId);
        if (unitOfWork != null && unitOfWork.getId() != null) {
            span.setAttribute("unitOfWork.id", unitOfWork.getId());
        }
        span.end();
    }

    // --- Compensation Span Management ---
    public void startSpanForCompensation(String executionId, String func) {
        if (rootSpan == null) {
            return;
        }
        String invocationKey = compensationKey(executionId, func);
        Span span = tracer.spanBuilder(invocationKey)
                .setParent(Context.current().with(rootSpan))
                .setSpanKind(SpanKind.INTERNAL)
                .startSpan();

        span.setAttribute("functionality", func);
        span.setAttribute("executionId", executionId);

        functionalitySpans.put(invocationKey, span);
    }

    public void startQueueWaitTimer(String executionId, String methodName) {
        String key = commandKey(executionId, methodName);
        queueWaitTimesNano.put(key, System.nanoTime());
    }

    public void endQueueWaitTimer(String executionId, String methodName, String microserviceName) {
        String key = commandKey(executionId, methodName);
        Long start = queueWaitTimesNano.remove(key);
        double durationMs = -1D;
        if (start != null) {
            durationMs = (System.nanoTime() - start) / 1_000_000.0;
        }

        // Fetch command span by adding command at the end of the method
        Span parentSpan = getCommandSpan(executionId, methodName + "command");
        if (parentSpan != null) {
            parentSpan.setAttribute("queue time (ms)", durationMs);
        }
    }

    public void endSpanForCompensation(String executionId, String func) {
        String invocationKey = compensationKey(executionId, func);
        Span span = functionalitySpans.remove(invocationKey);
        if (span == null) {
            return;
        }

        forceEndAllActiveCommandsSpans(executionId);
        span.end();
    }

    // --- Command Span Management ---
    public Span getCommandSpan(String executionId, String commandName) {
        String key = commandKey(executionId, commandName);
        return commandSpans.get(key);
    }

    public void startCommandSpan(String executionId, Command command) {
        String commandName = command.getClass().getSimpleName();
        Span parentSpan = functionalitySpans.get(executionId);
        if (parentSpan == null) {
            return;
        }

        String key = commandKey(executionId, commandName);
        // get 1 if null otherwise add 1
        int attempt = commandRetryCounters.merge(key, 1, Integer::sum);
        int retryCount = attempt - 1;

        String spanName = retryCount > 0
                ? commandName + " [Retry #" + retryCount + "]"
                : commandName;

        Span commandSpan = tracer.spanBuilder(spanName)
                .setParent(Context.current().with(parentSpan))
                .setSpanKind(SpanKind.INTERNAL)
                .startSpan();

        commandSpan.setAttribute("command.name", commandName);
        commandSpan.setAttribute("executionId", executionId);
        commandSpan.setAttribute("microservice", command.getServiceName());
        if (command.getUnitOfWork() != null) {
            commandSpan.setAttribute("functionality", command.getUnitOfWork().getFunctionalityName());
        }
        commandSpans.put(key, commandSpan);
    }

    public void endCommandSpan(String executionId, Command command) {
        String commandName = command.getClass().getSimpleName();
        String key = commandKey(executionId, commandName);
        Span commandSpan = commandSpans.remove(key);
        if (commandSpan != null) {
            commandSpan.end();
        }
    }

    // --- Delay Span Management ---
    public Span startDelaySpan(String executionId, String command, int delay, boolean isBefore) {
        if (delay <= 0) {
            return null;
        }
        Span parentSpan = getCommandSpan(executionId, command);
        if (parentSpan == null)
            return null;
        String spanName = (isBefore ? "before" : "after");
        Span span = tracer.spanBuilder(spanName)
                .setParent(Context.current().with(parentSpan))
                .setSpanKind(SpanKind.INTERNAL)
                .startSpan();

        span.setAttribute("executionId", executionId);
        span.setAttribute("command", command);
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

    public void recordException(String executionId, Throwable e, String message) {
        Span span = getSpanForFunctionality(executionId);
        if (span == null)
            return;
        span.recordException(e);
        span.setStatus(StatusCode.ERROR, message != null ? message : e.getMessage());
        span.addEvent("exception.caught", Attributes.of(
                AttributeKey.stringKey("exception.type"), e.getClass().getSimpleName(),
                AttributeKey.stringKey("exception.message"), e.getMessage()));
    }

    public void recordCommandException(String executionId, String commandName, Throwable e,
            String message) {
        Span span = getCommandSpan(executionId, commandName);
        if (span == null)
            return;
        span.recordException(e);
        span.setStatus(StatusCode.ERROR, message != null ? message : e.getMessage());
        span.setAttribute("retry.failed", true);
        span.addEvent("exception.caught", Attributes.of(
                AttributeKey.stringKey("exception.type"), e.getClass().getSimpleName(),
                AttributeKey.stringKey("exception.message"), e.getMessage()));
    }

    public void recordWarning(String executionId, Exception e, String message) {
        Span span = getSpanForFunctionality(executionId);
        if (span == null)
            return;
        span.addEvent("warning", Attributes.of(
                AttributeKey.stringKey("exception.type"), e.getClass().getSimpleName(),
                AttributeKey.stringKey("message"), message));
    }

    public void setSpanAttribute(String executionId, String key, Boolean value) {
        Span span = getSpanForFunctionality(executionId);
        if (span != null) {
            span.setAttribute(key, value);
        }
    }

    public String resolveExecutionId(UnitOfWork unitOfWork) {
        if (unitOfWork == null) {
            return "null-" + UUID.randomUUID().toString().substring(0, 8);
        }

        return uowTraceIds.computeIfAbsent(unitOfWork, key -> UUID.randomUUID().toString().substring(0, 8));
    }

    // --- Private Helpers ---
    private Span getSpanForFunctionality(String executionId) {
        return functionalitySpans.get(executionId);
    }

    private void forceEndAllActiveCommandsSpans(String executionId) {
        String prefix = executionId + "::";
        List<String> commandSpansList = commandSpans.keySet().stream()
                .filter(key -> key.startsWith(prefix))
                .collect(Collectors.toList());
        for (String key : commandSpansList) {
            Span commandSpan = commandSpans.get(key);
            if (commandSpan != null) {
                commandSpan.setAttribute("Warning", "Forced end command span");
                commandSpan.addEvent("forced-end", Attributes.of(
                        AttributeKey.stringKey("reason"), "Functionality was aborted!"));
                commandSpan.end();
                commandSpans.remove(key);
            }
        }
        commandRetryCounters.keySet().removeIf(key -> key.startsWith(prefix));
    }

    private String commandKey(String executionId, String commandName) {
        return (executionId + "::" + commandName).toLowerCase();
    }

    private String compensationKey(String executionId, String func) {
        return "[Compensate] " + func + "::" + executionId;
    }
}
