package pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.scenario.model;

import pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.scenario.ScenarioGeneratorConfig;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public record ScenarioGenerationResult(
        String schemaVersion,
        ScenarioGeneratorConfig effectiveConfig,
        List<ScenarioPlan> scenarioPlans,
        List<RejectedInputVariant> rejectedInputVariants,
        Map<String, Integer> counts,
        List<String> warnings) {

    public ScenarioGenerationResult {
        schemaVersion = schemaVersion == null || schemaVersion.isBlank()
                ? ScenarioPlan.SCHEMA_VERSION
                : schemaVersion;
        effectiveConfig = effectiveConfig == null ? new ScenarioGeneratorConfig() : effectiveConfig;
        scenarioPlans = scenarioPlans == null ? List.of() : List.copyOf(scenarioPlans);
        rejectedInputVariants = rejectedInputVariants == null ? List.of() : List.copyOf(rejectedInputVariants);
        counts = counts == null ? Map.of() : Collections.unmodifiableMap(new LinkedHashMap<>(counts));
        warnings = warnings == null ? List.of() : List.copyOf(warnings);
    }

    public ScenarioGenerationResult(String schemaVersion,
                                    ScenarioGeneratorConfig effectiveConfig,
                                    List<ScenarioPlan> scenarioPlans,
                                    Map<String, Integer> counts,
                                    List<String> warnings) {
        this(schemaVersion, effectiveConfig, scenarioPlans, List.of(), counts, warnings);
    }
}
