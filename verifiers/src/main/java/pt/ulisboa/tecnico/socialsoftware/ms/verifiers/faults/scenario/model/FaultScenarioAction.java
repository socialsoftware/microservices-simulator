package pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.scenario.model;

public record FaultScenarioAction(
        String deterministicId,
        FaultScenarioActionKind kind,
        String sagaInstanceId,
        String sourceFaultSlotId,
        String sourceCompensationCheckpointId,
        String occurrenceId) {

    public FaultScenarioAction {
        deterministicId = normalize(deterministicId);
        sagaInstanceId = normalize(sagaInstanceId);
        sourceFaultSlotId = normalize(sourceFaultSlotId);
        sourceCompensationCheckpointId = normalize(sourceCompensationCheckpointId);
        occurrenceId = normalize(occurrenceId);
    }

    private static String normalize(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
