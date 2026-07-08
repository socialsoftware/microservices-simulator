package pt.ulisboa.tecnico.socialsoftware.ms.faults;

public record FaultVectorBoundaryContext(
        String scenarioExecutionId,
        String scenarioPlanId,
        String sagaInstanceId,
        String scheduledStepId,
        Integer slotIndex,
        String functionalityClassFqn,
        String functionalityClassSimpleName,
        String runtimeStepName,
        int assignedBit) {
}
