package pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.scenario.model;

public record FaultScenarioAction(
        String deterministicId,
        FaultScenarioActionKind kind,
        String sagaInstanceId,
        String sourceFaultSlotId,
        String sourceCompensationCheckpointId,
        String sourceEventConsequenceId,
        String occurrenceId) {

    public FaultScenarioAction {
        deterministicId = normalize(deterministicId);
        sagaInstanceId = normalize(sagaInstanceId);
        sourceFaultSlotId = normalize(sourceFaultSlotId);
        sourceCompensationCheckpointId = normalize(sourceCompensationCheckpointId);
        sourceEventConsequenceId = normalize(sourceEventConsequenceId);
        occurrenceId = normalize(occurrenceId);
    }

    public FaultScenarioAction(String deterministicId,
                               FaultScenarioActionKind kind,
                               String sagaInstanceId,
                               String sourceFaultSlotId,
                               String sourceCompensationCheckpointId,
                               String occurrenceId) {
        this(deterministicId, kind, sagaInstanceId, sourceFaultSlotId,
                sourceCompensationCheckpointId, null, occurrenceId);
    }

    private static String normalize(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
