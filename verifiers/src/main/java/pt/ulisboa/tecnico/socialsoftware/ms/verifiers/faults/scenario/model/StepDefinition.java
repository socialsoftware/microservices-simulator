package pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.scenario.model;

import java.util.List;

public record StepDefinition(
        String deterministicId,
        String stepKey,
        String name,
        int orderIndex,
        List<String> predecessorStepKeys,
        List<StepFootprint> footprints,
        List<String> warnings) {

    public StepDefinition {
        deterministicId = normalize(deterministicId);
        stepKey = normalize(stepKey);
        name = normalize(name);
        predecessorStepKeys = predecessorStepKeys == null ? List.of() : List.copyOf(predecessorStepKeys);
        footprints = footprints == null ? List.of() : List.copyOf(footprints);
        warnings = warnings == null ? List.of() : List.copyOf(warnings);
    }

    private static String normalize(String value) {
        return value == null || value.isBlank() ? null : value;
    }
}
