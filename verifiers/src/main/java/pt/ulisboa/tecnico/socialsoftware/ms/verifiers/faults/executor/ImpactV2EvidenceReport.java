package pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.executor;

import com.fasterxml.jackson.annotation.JsonInclude;
import pt.ulisboa.tecnico.socialsoftware.ms.monitoring.impact.ImpactEvidence;

import java.util.List;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record ImpactV2EvidenceReport(
        String schemaVersion,
        String executionAttemptId,
        String packageManifestPath,
        String workloadPlanId,
        String faultScenarioId,
        String executionTerminalStatus,
        String scheduleConformance,
        String assessmentStatus,
        String assessmentReason,
        String collectionStatus,
        String collectionReason,
        String horizon,
        @JsonInclude(JsonInclude.Include.ALWAYS)
        Integer completeScore,
        @JsonInclude(JsonInclude.Include.ALWAYS)
        Integer observedAffectedObjectCount,
        List<CategoryResult> categoryResults,
        List<ImpactEvidence.AggregateSnapshot> baseline,
        List<ImpactEvidence.AggregateSnapshot> finalState,
        List<ImpactEvidence.CommittedWrite> committedWrites,
        List<ImpactEvidence.EventDelivery> eventDeliveries,
        List<ImpactEvidence.CoverageGap> coverageGaps,
        String residualAssessmentPolicy) {

    public static final String SCHEMA_VERSION =
            "microservices-simulator.scenario-impact-v2-assessment.v1";
    public static final String RESIDUAL_POLICY = "exclusive-keyed-list-fields-v4";
    public static final String LEGACY_RESIDUAL_POLICY = "whole-object-single-writer-v1";

    public ImpactV2EvidenceReport {
        schemaVersion = schemaVersion == null ? SCHEMA_VERSION : schemaVersion;
        categoryResults = copy(categoryResults);
        baseline = copy(baseline);
        finalState = copy(finalState);
        committedWrites = copy(committedWrites);
        eventDeliveries = copy(eventDeliveries);
        coverageGaps = copy(coverageGaps);
        // Reading a retained report must not label its old assessment with the new rule.
        residualAssessmentPolicy = residualAssessmentPolicy == null
                ? LEGACY_RESIDUAL_POLICY : residualAssessmentPolicy;
    }

    public record CategoryResult(
            String category,
            String coverageStatus,
            int candidateCount,
            List<Candidate> candidates,
            int positiveObjectCount,
            List<Finding> findings,
            List<UnknownReason> unknownReasons) {
        public CategoryResult {
            candidates = copy(candidates);
            findings = copy(findings);
            unknownReasons = copy(unknownReasons);
        }
    }

    public record Candidate(
            ImpactEvidence.AggregateIdentity aggregate,
            Integer eventId) {
    }

    public record Finding(
            String category,
            String reason,
            ImpactEvidence.AggregateIdentity affectedObject,
            ImpactEvidence.AggregateIdentity relatedObject,
            Integer eventId,
            List<String> actionIds,
            List<Long> versions,
            @JsonInclude(JsonInclude.Include.NON_EMPTY) List<String> affectedFields) {
        public Finding(String category, String reason, ImpactEvidence.AggregateIdentity affectedObject,
                       ImpactEvidence.AggregateIdentity relatedObject, Integer eventId,
                       List<String> actionIds, List<Long> versions) {
            this(category, reason, affectedObject, relatedObject, eventId, actionIds, versions, List.of());
        }

        public Finding {
            actionIds = copy(actionIds);
            versions = copy(versions);
            affectedFields = copy(affectedFields);
        }
    }

    public record UnknownReason(
            String category,
            String reason,
            String subject,
            ImpactEvidence.AggregateIdentity affectedObject,
            Integer eventId) {
    }

    private static <T> List<T> copy(List<T> values) {
        return values == null ? List.of() : List.copyOf(values);
    }
}
