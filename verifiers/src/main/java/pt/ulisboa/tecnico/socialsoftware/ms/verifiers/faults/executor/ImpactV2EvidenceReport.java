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
        List<ImpactEvidence.CoverageGap> coverageGaps) {

    public static final String SCHEMA_VERSION =
            "microservices-simulator.scenario-impact-v2-assessment.v1";

    public ImpactV2EvidenceReport {
        schemaVersion = schemaVersion == null ? SCHEMA_VERSION : schemaVersion;
        categoryResults = copy(categoryResults);
        baseline = copy(baseline);
        finalState = copy(finalState);
        committedWrites = copy(committedWrites);
        eventDeliveries = copy(eventDeliveries);
        coverageGaps = copy(coverageGaps);
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
            List<Long> versions) {
        public Finding {
            actionIds = copy(actionIds);
            versions = copy(versions);
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
