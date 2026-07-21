package pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.scenario.model;

import java.util.List;

public record CompensationCheckpoint(
        String deterministicId,
        int checkpointIndex,
        String sagaInstanceId,
        String sourceScheduledStepId,
        String stepId,
        String runtimeStepName,
        String occurrenceId,
        CompensationEvidenceClass evidenceClass,
        List<StepFootprint> forwardFootprints,
        List<StepFootprint> compensationFootprints,
        List<String> warnings) {

    public CompensationCheckpoint {
        deterministicId = normalize(deterministicId);
        sagaInstanceId = normalize(sagaInstanceId);
        sourceScheduledStepId = normalize(sourceScheduledStepId);
        stepId = normalize(stepId);
        runtimeStepName = normalize(runtimeStepName);
        occurrenceId = normalize(occurrenceId);
        forwardFootprints = forwardFootprints == null ? List.of() : List.copyOf(forwardFootprints);
        compensationFootprints = compensationFootprints == null ? List.of() : List.copyOf(compensationFootprints);
        warnings = warnings == null ? List.of() : List.copyOf(warnings);
    }

    private static String normalize(String value) {
        return value == null || value.isBlank() ? null : value;
    }
}
