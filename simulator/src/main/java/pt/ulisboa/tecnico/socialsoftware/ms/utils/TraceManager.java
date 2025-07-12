package pt.ulisboa.tecnico.socialsoftware.ms.utils;

import pt.ulisboa.tecnico.socialsoftware.ms.utils.BehaviourService;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import io.opentelemetry.api.GlobalOpenTelemetry;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.SpanKind;
import io.opentelemetry.api.trace.Tracer;
import io.opentelemetry.context.Context;
import io.opentelemetry.context.Scope;
import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.sdk.OpenTelemetrySdk;
import io.opentelemetry.sdk.trace.SdkTracerProvider;
import io.opentelemetry.sdk.trace.export.BatchSpanProcessor;
import io.opentelemetry.exporter.otlp.trace.OtlpGrpcSpanExporter;
import io.opentelemetry.sdk.resources.Resource;
import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.api.common.AttributeKey;
import io.opentelemetry.api.trace.StatusCode;

public class TraceManager {

    private static final TraceManager INSTANCE = new TraceManager();
    private final Tracer tracer;
    private final SdkTracerProvider tracerProvider;
    private static Span rootSpan;
    private static int rootSpanId = 1;

     // Map to store active spans by functionality
    private final Map<String, Span> functionalitySpans = new ConcurrentHashMap<>();

    // Store step child spans per (functionality + step)
    private final Map<String, Span> stepSpans = new ConcurrentHashMap<>();

    private TraceManager() {
        OtlpGrpcSpanExporter spanExporter = OtlpGrpcSpanExporter.builder()
            .setEndpoint("http://localhost:4317")  // or your collector address
            .build();

        Resource serviceResource = Resource.getDefault().merge(
            Resource.create(
                Attributes.of(AttributeKey.stringKey("service.name"), "my-app")
            )
        );

        SdkTracerProvider tracerProvider = SdkTracerProvider.builder()
            .setResource(serviceResource)
            .addSpanProcessor(BatchSpanProcessor.builder(spanExporter).build())
            .build();

        this.tracerProvider = tracerProvider;

        OpenTelemetry openTelemetry = OpenTelemetrySdk.builder()
            .setTracerProvider(tracerProvider)
            .buildAndRegisterGlobal();

        this.tracer = GlobalOpenTelemetry.getTracer("my-app");
    }

    public static TraceManager getInstance() {
        return INSTANCE;
    }

    public Span getSpanForFunctionality(String func) {
        return functionalitySpans.get(func);
    }

    public void startRootSpan() {
        String rootSpanName = "root" + rootSpanId;
        Span span = tracer.spanBuilder(rootSpanName)
                        .setSpanKind(SpanKind.INTERNAL)
                        .startSpan();
        rootSpan = span;
        span.setAttribute("root", "root");
        rootSpanId++;
    }

    public void endRootSpan() {
        if (rootSpan != null) {
            rootSpan.end();
            rootSpan = null;
        } else {
            throw new IllegalStateException("Root span has not been started");
        }
    }

    private void forceEndAllActiveStepsSpans(String func) {
        String prefix = func + "::";
        List<String> stepSpansList = stepSpans.keySet().stream()
                .filter(key -> key.startsWith(prefix))
                .collect(Collectors.toList());
        for (String key : stepSpansList) {
            Span stepSpan = getStepSpan(func, key.substring(prefix.length()));
            stepSpan.setAttribute("Warning", "Forced end step span");
            stepSpan.addEvent("forced-end", Attributes.of(
                AttributeKey.stringKey("reason"), "Functionality was aborted!"
            ));
            endStepSpan(func, key.substring(prefix.length()));
        }
    }

    public void startSpanForFunctionality(String func) {
        if (rootSpan == null) {
            return; // or throw an exception if you want to enforce starting root span first
            //throw new IllegalStateException("Root span must be started before starting functionality spans");
        }
        String name = func + "::" + BehaviourService.getFuncCounter(func);
        Span span = tracer.spanBuilder(name)
                        .setParent(Context.current().with(rootSpan))
                        .setSpanKind(SpanKind.INTERNAL)
                        .startSpan();
        
        span.setAttribute("func.name", name);
        span.setAttribute("functionality", func);

        functionalitySpans.put(func, span);
    }
        
                    
    public void endSpanForFunctionality(String func) {
        forceEndAllActiveStepsSpans(func);
        functionalitySpans.computeIfPresent(func, (f, span) -> {
            span.end();
            return null;
            }
        );
    }

    public void startSpanForCompensation(String func) {
        if (rootSpan == null) {
            return; // or throw an exception if you want to enforce starting root span first
            //throw new IllegalStateException("Root span must be started before starting functionality spans");
        }
        String name = "[COMPENSATE] "+ func + "::" + (BehaviourService.getFuncCounter(func) - 1);
        Span span = tracer.spanBuilder(name)
                        .setParent(Context.current().with(rootSpan))
                        .setSpanKind(SpanKind.INTERNAL)
                        .startSpan();
        
        span.setAttribute("func.name", name);
        span.setAttribute("functionality", func);


        functionalitySpans.put(func+"Compensate", span);
    }
        
                    
    public void endSpanForCompensation(String func) {
        functionalitySpans.computeIfPresent(func+"Compensate", (f, span) -> {
            span.end();
            return null;
            }
        );
    }

    private String key(String func, String stepName) {
        return func + "::" + stepName;
    }

    // Access current step span (for logging or context)
    public Span getStepSpan(String func, String stepName) {
        return stepSpans.get(key(func, stepName));
    }
    
    // Start a step span as child of its functionality
    public void startStepSpan(String func, String stepName) {
        Span parentSpan = functionalitySpans.get(func);

        if (parentSpan == null) {
            //throw new IllegalStateException("Functionality span not started: " + func);
            return; // or handle as needed
        }

        Span stepSpan = tracer.spanBuilder(stepName)
                .setParent(Context.current().with(parentSpan))
                .setSpanKind(SpanKind.INTERNAL)
                .startSpan();

        stepSpan.setAttribute("step.name", stepName);
        stepSpan.setAttribute("functionality", func);

        stepSpans.put(key(func, stepName), stepSpan);
    }

    public void endStepSpan(String func, String stepName) {
        String key = key(func, stepName);
        Span stepSpan = stepSpans.remove(key);
        if (stepSpan != null) {
            stepSpan.end();
        }
    }

    public Span startDelaySpan(String func, String step, int delay, boolean isBefore) {
        if (delay <= 0) {
            return null; // No delay, no span needed
        }
        Span parentSpan = getStepSpan(func, step);
        if (parentSpan == null) return null;
        String spanName = (isBefore ? "before" : "after");
        Span span = tracer.spanBuilder(spanName)
                        .setParent(Context.current().with(parentSpan))
                        .setSpanKind(SpanKind.INTERNAL)
                        .startSpan();
        
        span.setAttribute("functionality", func);
        span.setAttribute("step", step);
        span.setAttribute("value", Integer.toString(delay)+ " ms");

        return span;
    }

    public void endDelaySpan(Span delaySpan) {
        if (delaySpan != null) {
            delaySpan.end();
        }
    }

    public void forceFlush() {
        tracerProvider.forceFlush().join(2, TimeUnit.SECONDS);
    }

    public void recordException(String func, Throwable e, String message) {
    Span span = getSpanForFunctionality(func);

    if (span == null) return;

    span.recordException(e);
    span.setStatus(StatusCode.ERROR, message != null ? message : e.getMessage());
    span.addEvent("exception.caught", Attributes.of(
        AttributeKey.stringKey("exception.type"), e.getClass().getSimpleName(),
        AttributeKey.stringKey("exception.message"), e.getMessage()
    ));
    }

    public void recordWarning(String func, Exception e, String message) {
    Span span = getSpanForFunctionality(func);

    if (span == null) return;

    span.addEvent("warning", Attributes.of(
        AttributeKey.stringKey("exception.type"), e.getClass().getSimpleName(),
        AttributeKey.stringKey("message"), message
    ));
    }

    public void setSpanAttribute(String func, String key, String value) {
        Span span = getSpanForFunctionality(func);
        if (span != null) {
            span.setAttribute(key, value);
        }
    }

}