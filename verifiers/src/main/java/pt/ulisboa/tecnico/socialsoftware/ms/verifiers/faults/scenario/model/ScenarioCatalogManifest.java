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
        Map<String, Integer> counts,
        List<String> warnings,
        String catalogPath,
        String manifestPath,
        String rejectedInputsPath,
        Map<String, Integer> inputVariantsBySourceMode,
        Map<String, Integer> inputVariantsAcceptedBySourceMode,
        Map<String, Integer> inputVariantsRejectedBySourceModeReason) {

    public ScenarioCatalogManifest {
        schemaVersion = schemaVersion == null || schemaVersion.isBlank()
                ? ScenarioPlan.SCHEMA_VERSION
                : schemaVersion;
        generatedAt = normalize(generatedAt);
        effectiveConfig = effectiveConfig == null ? new ScenarioGeneratorConfig() : effectiveConfig;
        counts = orderedMap(counts);
        warnings = warnings == null ? List.of() : List.copyOf(warnings);
        catalogPath = normalize(catalogPath);
        manifestPath = normalize(manifestPath);
        rejectedInputsPath = normalize(rejectedInputsPath);
        inputVariantsBySourceMode = orderedMap(inputVariantsBySourceMode);
        inputVariantsAcceptedBySourceMode = orderedMap(inputVariantsAcceptedBySourceMode);
        inputVariantsRejectedBySourceModeReason = orderedMap(inputVariantsRejectedBySourceModeReason);
    }

    public ScenarioCatalogManifest(String schemaVersion,
                                   String generatedAt,
                                   ScenarioGeneratorConfig effectiveConfig,
                                   Map<String, Integer> counts,
                                   List<String> warnings,
                                   String catalogPath,
                                   String manifestPath) {
        this(schemaVersion,
                generatedAt,
                effectiveConfig,
                counts,
                warnings,
                catalogPath,
                manifestPath,
                null,
                Map.of(),
                Map.of(),
                Map.of());
    }

    private static Map<String, Integer> orderedMap(Map<String, Integer> values) {
        return values == null ? Map.of() : Collections.unmodifiableMap(new LinkedHashMap<>(values));
    }

    private static String normalize(String value) {
        return value == null || value.isBlank() ? null : value;
    }
}
