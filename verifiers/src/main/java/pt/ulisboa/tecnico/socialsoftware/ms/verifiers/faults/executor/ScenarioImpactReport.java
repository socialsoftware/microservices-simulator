package pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.executor;

import java.util.List;

public record ScenarioImpactReport(
        String schemaVersion,
        String impactModel,
        String evaluationStatus,
        String notEvaluatedReason,
        String executionAttemptId,
        String workloadPlanId,
        String faultScenarioId,
        Integer invariantViolationCount,
        Integer impactScore,
        List<InvariantViolationFinding> findings) {

    public static final String SCHEMA_VERSION = "microservices-simulator.scenario-impact-report.v1";
    public static final String IMPACT_MODEL = "ImpactV1";

    public ScenarioImpactReport {
        findings = findings == null ? List.of() : List.copyOf(findings);
    }

    public static ScenarioImpactReport evaluate(ScenarioExecutionReport executionReport,
                                                List<InvariantViolationFinding> findings) {
        List<InvariantViolationFinding> stableFindings = findings == null ? List.of() : List.copyOf(findings);
        boolean evaluated = switch (executionReport.terminalStatus()) {
            case "SUCCESS", "COMPENSATED", "PARTIAL_COMPENSATED" -> true;
            default -> false;
        };
        Integer count = evaluated ? stableFindings.size() : null;
        return new ScenarioImpactReport(
                SCHEMA_VERSION,
                IMPACT_MODEL,
                evaluated ? "EVALUATED" : "NOT_EVALUATED",
                evaluated ? null : executionReport.terminalStatus(),
                executionReport.executionAttemptId(),
                executionReport.workloadPlanId(),
                executionReport.faultScenarioId(),
                count,
                count,
                stableFindings);
    }

    public record InvariantViolationFinding(
            long sequence,
            String eventId,
            String functionalityName,
            String functionalityClassFqn,
            String inputVariantId,
            String functionalityInvocationId,
            String stepName,
            String testClassFqn,
            String testMethodName,
            String testDisplayName,
            String testUniqueId,
            Long unitOfWorkVersion,
            String aggregateType,
            String aggregateId,
            String sourceMethod,
            String verificationMethod,
            String exceptionClass,
            String exceptionMessage,
            String executionAttemptId,
            String workloadPlanId,
            String sagaInstanceId,
            String scheduledStepId,
            Integer slotIndex,
            String runtimeStepName) {
    }
}
