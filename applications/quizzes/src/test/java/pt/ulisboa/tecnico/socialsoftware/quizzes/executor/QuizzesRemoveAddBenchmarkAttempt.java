package pt.ulisboa.tecnico.socialsoftware.quizzes.executor;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.util.List;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record QuizzesRemoveAddBenchmarkAttempt(
        String schemaVersion,
        String benchmarkId,
        String observationRule,
        String classification,
        String validity,
        String validityReason,
        String packageManifestPath,
        String packageManifestSha256,
        String workloadPlanId,
        String faultScenarioId,
        String executionAttemptId,
        String assignedVector,
        List<PersistedAction> persistedActionOrder,
        String recoveryScheduleIdentity,
        String executionTerminalStatus,
        String scheduleConformance,
        ImpactV1 impactV1,
        ObservedAggregate tournament,
        ObservedAggregate referencedQuiz,
        boolean observationCompleted,
        Boolean brokenReference,
        String brokenReferenceReason,
        int repetition,
        RuntimeContext runtimeContext) {

    public static final String SCHEMA_VERSION =
            "microservices-simulator.quizzes-remove-add-benchmark-attempt.v3";
    public static final String BENCHMARK_ID = "quizzes-remove-tournament-add-participant";
    public static final String OBSERVATION_RULE =
            "An evaluated execution is harmful when an active Tournament still refers to a deleted Quiz.";

    public QuizzesRemoveAddBenchmarkAttempt {
        schemaVersion = schemaVersion == null ? SCHEMA_VERSION : schemaVersion;
        benchmarkId = benchmarkId == null ? BENCHMARK_ID : benchmarkId;
        observationRule = observationRule == null ? OBSERVATION_RULE : observationRule;
        persistedActionOrder = persistedActionOrder == null ? List.of() : List.copyOf(persistedActionOrder);
    }

    public record PersistedAction(
            int position,
            String actionId,
            String kind,
            String sagaInstanceId,
            String runtimeStepName,
            String compensationEvidenceClass) {
    }

    public record ImpactV1(
            String evaluationStatus,
            String notEvaluatedReason,
            Integer findingCount,
            Integer score) {
    }

    public record ObservedAggregate(
            Integer aggregateId,
            String aggregateType,
            String state,
            QuizzesPersistedSagaStateObserver.PersistedSagaState persistedSagaState,
            Integer referencedQuizAggregateId) {
    }

    public record RuntimeContext(
            String resetBoundary,
            String runtimeContextId,
            long processId,
            String database,
            String springProfiles,
            String javaVersion,
            String sourceRevision,
            String sourceTreeState) {
    }
}
