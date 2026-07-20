package pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.executor;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.util.List;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record ScenarioExecutionReport(
        String schemaVersion,
        String executionAttemptId,
        String terminalStatus,
        String packageManifestPath,
        String workloadPlanId,
        String faultScenarioId,
        String scenarioKind,
        String assignedVector,
        String providerMode,
        String scheduleConformance,
        RuntimeMetadata runtimeMetadata,
        List<FaultSlot> faultSlots,
        List<PlannedAction> plannedActions,
        List<ActionOutcome> actualActions,
        List<LifecycleEvent> lifecycleEvents,
        List<Participant> participants,
        List<Blocker> blockers) {

    public static final String SCHEMA_VERSION = "microservices-simulator.scenario-execution-report.v4";

    public ScenarioExecutionReport {
        schemaVersion = schemaVersion == null || schemaVersion.isBlank() ? SCHEMA_VERSION : schemaVersion;
        providerMode = providerMode == null || providerMode.isBlank() ? "NONE" : providerMode;
        faultSlots = copy(faultSlots);
        plannedActions = copy(plannedActions);
        actualActions = copy(actualActions);
        lifecycleEvents = copy(lifecycleEvents);
        participants = copy(participants);
        blockers = copy(blockers);
    }

    private static <T> List<T> copy(List<T> values) {
        return values == null ? List.of() : List.copyOf(values);
    }

    public record RuntimeMetadata(
            String applicationBase,
            String applicationId,
            String springApplicationClass,
            String springProfiles,
            String mavenProfile,
            String packageManifestPath,
            String faultScenarioId,
            String executorMode,
            boolean dryRun) {
    }

    public record FaultSlot(
            int slotIndex,
            String faultSlotId,
            String scheduledStepId,
            String stepId,
            int scheduleOrder,
            String sagaInstanceId,
            String runtimeStepName,
            int assignedBit,
            String state,
            String reason) {
    }

    public record PlannedAction(
            String actionId,
            String kind,
            String sagaInstanceId,
            String sourceFaultSlotId,
            String sourceCompensationCheckpointId,
            String sourceScheduledStepId,
            String sourceStepId,
            String runtimeStepName,
            String compensationEvidenceClass,
            int plannedPosition) {
    }

    public record ActionOutcome(
            String actionId,
            String kind,
            String sagaInstanceId,
            String sourceFaultSlotId,
            String sourceCompensationCheckpointId,
            String sourceScheduledStepId,
            String sourceStepId,
            String runtimeStepName,
            String compensationEvidenceClass,
            int plannedPosition,
            int actualPosition,
            String status,
            String bodyOutcome,
            String commitOutcome,
            String faultOrigin,
            List<RecoverySubOutcome> recoverySubOutcomes,
            String exceptionClass,
            String exceptionMessage) {
        public ActionOutcome {
            recoverySubOutcomes = copy(recoverySubOutcomes);
        }
    }

    public record RecoverySubOutcome(String kind, String status) {
    }

    public record LifecycleEvent(
            int sequence,
            String sagaInstanceId,
            String type,
            String actionId,
            String outcome,
            String exceptionClass,
            String exceptionMessage) {
    }

    public record Participant(
            String sagaInstanceId,
            String sagaFqn,
            String inputVariantId,
            String materializationState,
            String startupState,
            String finalState,
            List<SkippedForwardAction> skippedForwardActions,
            List<Blocker> blockers) {
        public Participant {
            materializationState = normalizeState(materializationState, "NOT_ATTEMPTED");
            startupState = normalizeState(startupState, "NOT_ATTEMPTED");
            finalState = normalizeState(finalState, "NOT_STARTED");
            skippedForwardActions = copy(skippedForwardActions);
            blockers = copy(blockers);
        }
    }

    public record SkippedForwardAction(
            String faultSlotId,
            String scheduledStepId,
            String stepId,
            int scheduleOrder,
            String runtimeStepName,
            int assignedBit,
            String state,
            String reason) {
    }

    public record Blocker(
            String workloadPlanId,
            String faultScenarioId,
            String inputVariantId,
            Integer argumentIndex,
            String actionId,
            String sourceScheduledStepId,
            String reason,
            String message) {
        public Blocker(String workloadPlanId,
                       String inputVariantId,
                       Integer argumentIndex,
                       String sourceScheduledStepId,
                       String reason,
                       String message) {
            this(workloadPlanId, null, inputVariantId, argumentIndex, null, sourceScheduledStepId, reason, message);
        }
    }

    private static String normalizeState(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value;
    }
}
