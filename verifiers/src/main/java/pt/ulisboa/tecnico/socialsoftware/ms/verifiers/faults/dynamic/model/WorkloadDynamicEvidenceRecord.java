package pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.dynamic.model;

import java.util.List;

public record WorkloadDynamicEvidenceRecord(
        String schemaVersion,
        String workloadPlanId,
        List<String> inputVariantIds,
        DynamicEvidenceSummary dynamicEvidence) {

    public static final String SCHEMA_VERSION = "microservices-simulator.workload-dynamic-evidence.v3";

    public WorkloadDynamicEvidenceRecord {
        schemaVersion = schemaVersion == null || schemaVersion.isBlank() ? SCHEMA_VERSION : schemaVersion;
        workloadPlanId = normalize(workloadPlanId);
        inputVariantIds = inputVariantIds == null ? List.of() : List.copyOf(inputVariantIds);
    }

    private static String normalize(String value) {
        return value == null || value.isBlank() ? null : value;
    }
}
