package pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.scenario.model;

public record FaultSlotGenerationDiagnostic(
        int slotIndex,
        String faultSlotId,
        String scheduledStepId,
        String sagaInstanceId,
        int assignedBit,
        FaultSlotGenerationState state) {
}
