package pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.executor;

import java.util.List;
import java.util.Map;

public record ScenarioExecutionReport(
        String schemaVersion,
        String scenarioExecutionId,
        String terminalStatus,
        String catalogPath,
        String catalogKind,
        String selectionMode,
        String selectionReason,
        String requestedScenarioPlanId,
        String scenarioPlanId,
        String scenarioKind,
        String assignedVector,
        String vectorSource,
        String providerMode,
        RuntimeMetadata runtimeMetadata,
        List<FaultSlot> faultSlots,
        Map<String, Integer> skippedCandidateCounts,
        List<Blocker> blockers,
        List<Participant> participants) {

    public static final String SCHEMA_VERSION = "microservices-simulator.scenario-execution-report.v3";

    public ScenarioExecutionReport {
        schemaVersion = schemaVersion == null || schemaVersion.isBlank() ? SCHEMA_VERSION : schemaVersion;
        terminalStatus = terminalStatus == null || terminalStatus.isBlank() ? "SUCCESS" : terminalStatus;
        providerMode = providerMode == null || providerMode.isBlank() ? "NONE" : providerMode;
        faultSlots = faultSlots == null ? List.of() : List.copyOf(faultSlots);
        skippedCandidateCounts = skippedCandidateCounts == null ? Map.of() : Map.copyOf(skippedCandidateCounts);
        blockers = blockers == null ? List.of() : List.copyOf(blockers);
        participants = participants == null ? List.of() : List.copyOf(participants);
    }

    public record RuntimeMetadata(
            String applicationBase,
            String applicationId,
            String springApplicationClass,
            String springProfiles,
            String mavenProfile,
            String catalogPath,
            String catalogKind,
            String scenarioPlanId,
            String vectorSource,
            String executorMode,
            boolean dryRun) {
    }

    public record Participant(
            String sagaInstanceId,
            String sagaFqn,
            String inputVariantId,
            String materializationState,
            String startupState,
            String lifecycleOutcome,
            List<StepOutcome> stepOutcomes,
            List<SkippedStep> skippedSteps,
            List<Blocker> blockers) {
        public Participant {
            materializationState = materializationState == null || materializationState.isBlank() ? "NOT_ATTEMPTED" : materializationState;
            startupState = startupState == null || startupState.isBlank() ? "NOT_ATTEMPTED" : startupState;
            lifecycleOutcome = lifecycleOutcome == null || lifecycleOutcome.isBlank() ? "NOT_STARTED" : lifecycleOutcome;
            stepOutcomes = stepOutcomes == null ? List.of() : List.copyOf(stepOutcomes);
            skippedSteps = skippedSteps == null ? List.of() : List.copyOf(skippedSteps);
            blockers = blockers == null ? List.of() : List.copyOf(blockers);
        }
    }

    public record FaultSlot(
            int slotIndex,
            String scheduledStepId,
            String catalogStepId,
            int scheduleOrder,
            String sagaInstanceId,
            String runtimeStepName,
            int assignedBit,
            String realizationState,
            String maskReason) {
    }

    public record StepOutcome(
            String scheduledStepId,
            String catalogStepId,
            int scheduleOrder,
            String runtimeStepName,
            String status,
            String exceptionClass,
            String exceptionMessage) {
    }

    public record SkippedStep(
            String scheduledStepId,
            String catalogStepId,
            int scheduleOrder,
            String runtimeStepName,
            String reason) {
    }

    public record Blocker(
            String scenarioPlanId,
            String inputVariantId,
            Integer argumentIndex,
            String scheduledStepId,
            String reason,
            String message) {
    }
}
