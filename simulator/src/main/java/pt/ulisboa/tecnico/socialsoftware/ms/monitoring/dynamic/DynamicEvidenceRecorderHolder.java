package pt.ulisboa.tecnico.socialsoftware.ms.monitoring.dynamic;

import pt.ulisboa.tecnico.socialsoftware.ms.aggregate.Aggregate;
import pt.ulisboa.tecnico.socialsoftware.ms.messaging.Command;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pt.ulisboa.tecnico.socialsoftware.ms.transactional.unitOfWork.UnitOfWork;

import java.util.LinkedHashMap;
import java.util.Map;

public final class DynamicEvidenceRecorderHolder {
    private static final Logger logger = LoggerFactory.getLogger(DynamicEvidenceRecorderHolder.class);
    private static volatile DynamicEvidenceRecorder recorder = new DynamicEvidenceNoopRecorder();

    private DynamicEvidenceRecorderHolder() {
    }

    public static DynamicEvidenceRecorder getRecorder() {
        return recorder;
    }

    public static void setRecorder(DynamicEvidenceRecorder recorder) {
        DynamicEvidenceRecorderHolder.recorder = recorder == null ? new DynamicEvidenceNoopRecorder() : recorder;
    }

    public static void recordStepStarted(DynamicEvidenceContext.StepContext context) {
        record(DynamicEvidenceEvent.of(
                "STEP_STARTED",
                context.functionalityName(),
                context.functionalityInvocationId(),
                context.stepName(),
                context.unitOfWorkVersion(),
                Map.of("stepPhase", "FORWARD")));
    }

    public static void recordStepFinished(DynamicEvidenceContext.StepContext context, String outcome, Throwable error) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("outcome", outcome);
        payload.put("durationMillis", (System.nanoTime() - context.startedAtNanos()) / 1_000_000L);
        if (error != null) {
            Throwable cause = unwrap(error);
            payload.put("errorType", cause.getClass().getName());
            if (cause.getMessage() != null) {
                payload.put("errorMessage", cause.getMessage());
            }
        }
        record(DynamicEvidenceEvent.of(
                "STEP_FINISHED",
                context.functionalityName(),
                context.functionalityInvocationId(),
                context.stepName(),
                context.unitOfWorkVersion(),
                payload));
    }

    public static void recordCommandSent(Command command, DynamicEvidenceProperties properties) {
        DynamicEvidenceRecorder current = recorder;
        if (!current.isEnabled()) {
            return;
        }
        record(new CommandEvidenceExtractor(properties)
                .buildCommandSentEvent(command, DynamicEvidenceContext.current().orElse(null)));
    }

    public static void recordAggregateAccessed(String accessMode, Aggregate aggregate, UnitOfWork unitOfWork,
                                               String sourceMethod) {
        DynamicEvidenceRecorder current = recorder;
        if (!current.isEnabled() || aggregate == null) {
            return;
        }

        DynamicEvidenceContext.StepContext context = DynamicEvidenceContext.current().orElse(null);
        String functionalityName = context != null
                ? context.functionalityName()
                : unitOfWork != null ? unitOfWork.getFunctionalityName() : null;
        Long unitOfWorkVersion = context != null
                ? context.unitOfWorkVersion()
                : unitOfWork != null ? unitOfWork.getVersion() : null;
        String invocationId = context != null
                ? context.functionalityInvocationId()
                : buildInvocationId(functionalityName, unitOfWorkVersion);
        String stepName = context != null ? context.stepName() : null;

        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("accessMode", accessMode);
        payload.put("aggregateType", aggregate.getAggregateType() != null
                ? aggregate.getAggregateType()
                : aggregate.getClass().getSimpleName());
        payload.put("aggregateId", aggregate.getAggregateId() == null ? null : String.valueOf(aggregate.getAggregateId()));
        payload.put("sourceMethod", sourceMethod);

        record(DynamicEvidenceEvent.of(
                "AGGREGATE_ACCESSED",
                functionalityName,
                invocationId,
                stepName,
                unitOfWorkVersion,
                payload));
    }

    private static void record(DynamicEvidenceEvent event) {
        DynamicEvidenceRecorder current = recorder;
        try {
            current.record(event);
        } catch (RuntimeException e) {
            logger.warn("Dynamic evidence recorder {} failed while recording {} event; swallowing to preserve domain behavior",
                    current.getClass().getSimpleName(), event.getEventKind(), e);
        }
    }

    private static String buildInvocationId(String functionalityName, Long unitOfWorkVersion) {
        if (functionalityName == null || unitOfWorkVersion == null) {
            return null;
        }
        return functionalityName + "-" + unitOfWorkVersion;
    }

    private static Throwable unwrap(Throwable error) {
        if (error instanceof java.util.concurrent.CompletionException && error.getCause() != null) {
            return error.getCause();
        }
        return error;
    }
}
