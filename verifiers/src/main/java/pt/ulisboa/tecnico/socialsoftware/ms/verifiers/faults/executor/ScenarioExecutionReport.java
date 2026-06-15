package pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.executor;

import java.util.List;
import java.util.Map;

public record ScenarioExecutionReport(
        String schemaVersion,
        String terminalStatus,
        String catalogPath,
        String catalogKind,
        String selectionMode,
        String selectionReason,
        String requestedScenarioPlanId,
        String scenarioPlanId,
        String sagaInstanceId,
        String sagaFqn,
        String inputVariantId,
        List<StepOutcome> stepOutcomes,
        Map<String, Integer> skippedCandidateCounts,
        List<Blocker> blockers) {

    public static final String SCHEMA_VERSION = "microservices-simulator.scenario-execution-report.v1";

    public ScenarioExecutionReport {
        schemaVersion = schemaVersion == null || schemaVersion.isBlank() ? SCHEMA_VERSION : schemaVersion;
        terminalStatus = terminalStatus == null || terminalStatus.isBlank() ? "SUCCESS" : terminalStatus;
        stepOutcomes = stepOutcomes == null ? List.of() : List.copyOf(stepOutcomes);
        skippedCandidateCounts = skippedCandidateCounts == null ? Map.of() : Map.copyOf(skippedCandidateCounts);
        blockers = blockers == null ? List.of() : List.copyOf(blockers);
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

    public record Blocker(
            String scenarioPlanId,
            String inputVariantId,
            Integer argumentIndex,
            String scheduledStepId,
            String reason,
            String message) {
    }
}
