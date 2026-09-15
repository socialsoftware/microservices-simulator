package pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.executor;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.JsonNode;
import pt.ulisboa.tecnico.socialsoftware.ms.monitoring.impact.ImpactEvidence;

import java.util.List;

/** Deterministic assessment of committed overwrites that reused observed constructor copies. */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record LostCopiedUpdateReport(
        String schemaVersion,
        List<Finding> findings,
        List<CoverageGap> coverageGaps,
        String scope) {

    public static final String SCHEMA_VERSION =
            "microservices-simulator.lost-copied-update-assessment.v1";
    public static final String SCOPE =
            "Observed unchanged scalar copies through inferred constructors; not complete lost-update detection.";

    public LostCopiedUpdateReport {
        schemaVersion = schemaVersion == null ? SCHEMA_VERSION : schemaVersion;
        findings = copy(findings);
        coverageGaps = copy(coverageGaps);
        scope = scope == null ? SCOPE : scope;
    }

    public record Finding(
            String findingId,
            ImpactEvidence.AggregateIdentity aggregate,
            Long beforeVersion,
            Long afterVersion,
            String phase,
            ImpactEvidence.Writer writer,
            List<FieldEvidence> fields) {
        public Finding {
            fields = copy(fields);
        }
    }

    public record FieldEvidence(
            String fieldPath,
            JsonNode oldValue,
            JsonNode overwrittenValue,
            long readOrder,
            long inputOrder,
            long constructorOrder,
            long foreignWriteOrder,
            long overwriteOrder,
            ImpactEvidence.Writer foreignWriter) {
    }

    public record CoverageGap(
            long order,
            String stage,
            String subject,
            String reason,
            String message) {
    }

    private static <T> List<T> copy(List<T> values) {
        return values == null ? List.of() : List.copyOf(values);
    }
}
