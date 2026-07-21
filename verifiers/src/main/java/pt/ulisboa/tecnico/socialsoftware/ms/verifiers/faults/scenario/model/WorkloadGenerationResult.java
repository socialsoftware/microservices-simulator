package pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.scenario.model;

import pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.scenario.ScenarioGeneratorConfig;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public record WorkloadGenerationResult(
        String schemaVersion,
        ScenarioGeneratorConfig effectiveConfig,
        List<WorkloadPlan> workloadPlans,
        List<RejectedInputVariant> rejectedInputVariants,
        Map<String, Integer> counts,
        List<String> warnings) {

    public WorkloadGenerationResult {
        schemaVersion = schemaVersion == null || schemaVersion.isBlank()
                ? WorkloadPlan.SCHEMA_VERSION
                : schemaVersion;
        effectiveConfig = effectiveConfig == null ? new ScenarioGeneratorConfig() : effectiveConfig;
        workloadPlans = workloadPlans == null ? List.of() : List.copyOf(workloadPlans);
        rejectedInputVariants = rejectedInputVariants == null ? List.of() : List.copyOf(rejectedInputVariants);
        counts = counts == null ? Map.of() : Collections.unmodifiableMap(new LinkedHashMap<>(counts));
        warnings = warnings == null ? List.of() : List.copyOf(warnings);
    }

    public WorkloadGenerationResult(String schemaVersion,
                                    ScenarioGeneratorConfig effectiveConfig,
                                    List<WorkloadPlan> workloadPlans,
                                    Map<String, Integer> counts,
                                    List<String> warnings) {
        this(schemaVersion, effectiveConfig, workloadPlans, List.of(), counts, warnings);
    }
}
