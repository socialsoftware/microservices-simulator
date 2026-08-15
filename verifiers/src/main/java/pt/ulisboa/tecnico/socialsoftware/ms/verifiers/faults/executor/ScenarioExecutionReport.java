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
        String deviationActionId,
        Integer deviationPlannedPosition,
        String deviationPolicy,
        String hardStopActionId,
        String hardStopReason,
        RuntimeMetadata runtimeMetadata,
        PrerequisiteSetup prerequisiteSetup,
        List<FaultSlot> faultSlots,
        List<PlannedAction> plannedActions,
        List<ActionOutcome> actualActions,
        List<LifecycleEvent> lifecycleEvents,
        List<Participant> participants,
        List<Blocker> blockers) {

    public static final String SCHEMA_VERSION = "microservices-simulator.scenario-execution-report.v5";

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
            String sourceEventConsequenceId,
            String sourceScheduledStepId,
            String sourceStepId,
            String runtimeStepName,
            String compensationEvidenceClass,
            String eventTypeFqn,
            String eventHandlingClassFqn,
            String eventHandlingMethodName,
            String eventHandlerClassFqn,
            String deliveryPolicy,
            int plannedPosition) {
    }

    public record ActionOutcome(
            String actionId,
            String kind,
            String sagaInstanceId,
            String sourceFaultSlotId,
            String sourceCompensationCheckpointId,
            String sourceEventConsequenceId,
            String sourceScheduledStepId,
            String sourceStepId,
            String runtimeStepName,
            String compensationEvidenceClass,
            String runtimeOccurrenceId,
            Integer plannedPosition,
            int actualPosition,
            String status,
            String bodyOutcome,
            String commitOutcome,
            String faultOrigin,
            EventRuntimeEvidence eventEvidence,
            List<RecoverySubOutcome> recoverySubOutcomes,
            String exceptionClass,
            String exceptionMessage) {
        public ActionOutcome {
            recoverySubOutcomes = copy(recoverySubOutcomes);
        }
    }

    public record EventRuntimeEvidence(
            Integer eventId,
            String eventTypeFqn,
            Integer publisherAggregateId,
            Long publisherAggregateVersion,
            Boolean published,
            Integer subscriberAggregateId,
            String eventHandlingClassFqn,
            String eventHandlingMethodName,
            String eventHandlerClassFqn) {
    }

    public record PrerequisiteSetup(
            String providerId,
            String providerVersion,
            String status,
            long durationNanos,
            long pendingEventsCleared,
            boolean emptyPendingEventBaseline,
            List<BaselineBinding> bindings,
            java.util.Map<String, String> evidence,
            String failureReason,
            String failureMessage) {
        public PrerequisiteSetup {
            bindings = copy(bindings);
            evidence = evidence == null ? java.util.Map.of()
                    : java.util.Collections.unmodifiableMap(new java.util.TreeMap<>(evidence));
        }
    }

    public record BaselineBinding(
            String key,
            String expectedTypeFqn,
            String actualTypeFqn,
            String status) {
    }

    public record RecoverySubOutcome(
            String kind,
            String status,
            String exceptionClass,
            String exceptionMessage) {
        public RecoverySubOutcome(String kind, String status) {
            this(kind, status, null, null);
        }
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
