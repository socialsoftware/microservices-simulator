package pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.scenario.model;

public record ForwardFaultSlot(
        String deterministicId,
        int slotIndex,
        String scheduledStepId,
        String sagaInstanceId,
        String stepId,
        String runtimeStepName,
        String occurrenceId) {

    public ForwardFaultSlot {
        deterministicId = normalize(deterministicId);
        scheduledStepId = normalize(scheduledStepId);
        sagaInstanceId = normalize(sagaInstanceId);
        stepId = normalize(stepId);
        runtimeStepName = normalize(runtimeStepName);
        occurrenceId = normalize(occurrenceId);
    }

    private static String normalize(String value) {
        return value == null || value.isBlank() ? null : value;
    }
}
