package pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.scenario.model;

import pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.scenario.ScenarioGeneratorConfig;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public record ScenarioCatalogManifest(
        String schemaVersion,
        String generatedAt,
        ScenarioGeneratorConfig effectiveConfig,
        String generationSource,
        String materializabilityPolicy,
        int recoveryScheduleCap,
        String faultScenarioVectorSource,
        List<WorkloadMaterializability> workloadMaterializability,
        Map<String, String> counts,
        List<String> warnings,
        ArtifactMetadata workloadCatalog,
        ArtifactMetadata faultScenarioCatalog,
        ArtifactMetadata scenarioSpaceAccounting,
        ArtifactMetadata rejectedInputsDiagnostic,
        Map<String, String> inputVariantsBySourceMode,
        Map<String, String> inputVariantsAcceptedBySourceMode,
        Map<String, String> inputVariantsRejectedBySourceModeReason) {

    public static final String SCHEMA_VERSION = "microservices-simulator.scenario-catalog-manifest.v3";
    public static final String FAULT_SCENARIO_SCHEMA_VERSION = FaultScenario.SCHEMA_VERSION;

    public ScenarioCatalogManifest {
        schemaVersion = schemaVersion == null || schemaVersion.isBlank() ? SCHEMA_VERSION : schemaVersion;
        generatedAt = normalize(generatedAt);
        effectiveConfig = effectiveConfig == null ? new ScenarioGeneratorConfig() : effectiveConfig;
        generationSource = normalize(generationSource);
        materializabilityPolicy = normalize(materializabilityPolicy);
        if (recoveryScheduleCap <= 0) {
            throw new IllegalArgumentException("recovery schedule cap must be a positive integer");
        }
        faultScenarioVectorSource = normalize(faultScenarioVectorSource);
        workloadMaterializability = workloadMaterializability == null ? List.of() : List.copyOf(workloadMaterializability);
        counts = orderedMap(counts);
        warnings = warnings == null ? List.of() : List.copyOf(warnings);
        inputVariantsBySourceMode = orderedMap(inputVariantsBySourceMode);
        inputVariantsAcceptedBySourceMode = orderedMap(inputVariantsAcceptedBySourceMode);
        inputVariantsRejectedBySourceModeReason = orderedMap(inputVariantsRejectedBySourceModeReason);
    }

    public record ArtifactMetadata(
            String artifactKind,
            String schemaVersion,
            String path,
            String recordCount) {
        public ArtifactMetadata {
            artifactKind = normalize(artifactKind);
            schemaVersion = normalize(schemaVersion);
            path = normalize(path);
            recordCount = recordCount == null || recordCount.isBlank() ? "0" : recordCount;
        }
    }

    private static Map<String, String> orderedMap(Map<String, String> values) {
        return values == null ? Map.of() : Collections.unmodifiableMap(new LinkedHashMap<>(values));
    }

    private static String normalize(String value) {
        return value == null || value.isBlank() ? null : value;
    }
}
