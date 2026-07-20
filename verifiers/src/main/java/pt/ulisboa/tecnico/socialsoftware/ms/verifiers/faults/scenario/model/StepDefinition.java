package pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.scenario.model;

import java.util.List;

public record StepDefinition(
        String deterministicId,
        String stepKey,
        String name,
        int orderIndex,
        List<String> predecessorStepKeys,
        List<StepFootprint> footprints,
        List<StepFootprint> compensationFootprints,
        boolean compensationRegistered,
        boolean forwardAnalysisComplete,
        boolean compensationAnalysisComplete,
        CompensationEvidenceClass compensationEvidence,
        List<String> analysisDiagnostics,
        List<String> warnings) {

    public StepDefinition(String deterministicId,
                          String stepKey,
                          String name,
                          int orderIndex,
                          List<String> predecessorStepKeys,
                          List<StepFootprint> footprints,
                          List<String> warnings) {
        this(deterministicId, stepKey, name, orderIndex, predecessorStepKeys, footprints, List.of(),
                false, true, true, null, List.of(), warnings);
    }

    public StepDefinition {
        deterministicId = normalize(deterministicId);
        stepKey = normalize(stepKey);
        name = normalize(name);
        predecessorStepKeys = predecessorStepKeys == null ? List.of() : List.copyOf(predecessorStepKeys);
        footprints = footprints == null ? List.of() : List.copyOf(footprints);
        compensationFootprints = compensationFootprints == null ? List.of() : List.copyOf(compensationFootprints);
        analysisDiagnostics = analysisDiagnostics == null ? List.of() : List.copyOf(analysisDiagnostics);
        warnings = warnings == null ? List.of() : List.copyOf(warnings);
    }

    private static String normalize(String value) {
        return value == null || value.isBlank() ? null : value;
    }
}
