package pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.dynamic.model;

import pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.scenario.model.ScenarioPlan;

public record EnrichedScenarioRecord(
        String schemaVersion,
        String scenarioPlanId,
        ScenarioPlan scenarioPlan,
        DynamicEvidenceSummary dynamicEvidence) {
    public static final String SCHEMA_VERSION = "microservices-simulator.scenario-catalog-enriched.v1";

    public EnrichedScenarioRecord {
        schemaVersion = schemaVersion == null || schemaVersion.isBlank() ? SCHEMA_VERSION : schemaVersion;
        scenarioPlanId = scenarioPlanId == null && scenarioPlan != null ? scenarioPlan.deterministicId() : scenarioPlanId;
    }
}
