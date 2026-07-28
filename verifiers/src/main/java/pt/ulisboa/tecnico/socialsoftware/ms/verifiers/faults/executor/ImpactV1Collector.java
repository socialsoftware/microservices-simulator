package pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.executor;

import pt.ulisboa.tecnico.socialsoftware.ms.monitoring.dynamic.DynamicEvidenceEvent;
import pt.ulisboa.tecnico.socialsoftware.ms.monitoring.dynamic.DynamicEvidenceRecorder;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;

final class ImpactV1Collector implements DynamicEvidenceRecorder {
    private static final String INVARIANT_VIOLATION = "INVARIANT_VIOLATION";

    private final DynamicEvidenceRecorder delegate;
    private final String expectedExecutionAttemptId;
    private final String expectedWorkloadPlanId;
    private final List<ScenarioImpactReport.InvariantViolationFinding> findings = new ArrayList<>();

    ImpactV1Collector(DynamicEvidenceRecorder delegate,
                      String expectedExecutionAttemptId,
                      String expectedWorkloadPlanId) {
        this.delegate = Objects.requireNonNull(delegate);
        this.expectedExecutionAttemptId = Objects.requireNonNull(expectedExecutionAttemptId);
        this.expectedWorkloadPlanId = expectedWorkloadPlanId;
    }

    @Override
    public boolean isEnabled() {
        return true;
    }

    @Override
    public synchronized void record(DynamicEvidenceEvent event) {
        if (event != null && INVARIANT_VIOLATION.equals(event.getEventKind()) && belongsToAttempt(event)) {
            findings.add(finding(findings.size() + 1L, event));
        }
        delegate.record(event);
    }

    List<ScenarioImpactReport.InvariantViolationFinding> findings() {
        synchronized (this) {
            return List.copyOf(findings);
        }
    }

    @Override
    public void close() {
        // The owning Spring/listener recorder outlives this attempt-scoped wrapper.
    }

    private boolean belongsToAttempt(DynamicEvidenceEvent event) {
        Map<String, Object> payload = event.getPayload();
        if (!expectedExecutionAttemptId.equals(text(payload, "executionAttemptId"))) {
            return false;
        }
        return expectedWorkloadPlanId == null
                || expectedWorkloadPlanId.equals(text(payload, "workloadPlanId"));
    }

    private ScenarioImpactReport.InvariantViolationFinding finding(long sequence, DynamicEvidenceEvent event) {
        Map<String, Object> payload = event.getPayload();
        return new ScenarioImpactReport.InvariantViolationFinding(
                sequence,
                event.getEventId(),
                event.getFunctionalityName(),
                event.getFunctionalityClassFqn(),
                event.getInputVariantId(),
                event.getFunctionalityInvocationId(),
                event.getStepName(),
                event.getTestClassFqn(),
                event.getTestMethodName(),
                event.getTestDisplayName(),
                event.getTestUniqueId(),
                event.getUnitOfWorkVersion(),
                text(payload, "aggregateType"),
                text(payload, "aggregateId"),
                text(payload, "sourceMethod"),
                text(payload, "verificationMethod"),
                text(payload, "exceptionClass"),
                text(payload, "exceptionMessage"),
                text(payload, "executionAttemptId"),
                text(payload, "workloadPlanId"),
                text(payload, "sagaInstanceId"),
                text(payload, "scheduledStepId"),
                integer(payload, "slotIndex"),
                text(payload, "runtimeStepName"));
    }

    private String text(Map<String, Object> payload, String key) {
        Object value = payload == null ? null : payload.get(key);
        return value == null ? null : String.valueOf(value);
    }

    private Integer integer(Map<String, Object> payload, String key) {
        Object value = payload == null ? null : payload.get(key);
        if (value instanceof Number number) {
            return number.intValue();
        }
        if (value == null) {
            return null;
        }
        try {
            return Integer.valueOf(String.valueOf(value));
        } catch (NumberFormatException ignored) {
            return null;
        }
    }
}
