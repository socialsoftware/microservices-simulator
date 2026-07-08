package pt.ulisboa.tecnico.socialsoftware.ms.faults;

public record FaultVectorFault(
        String scenarioExecutionId,
        String scenarioPlanId,
        String sagaInstanceId,
        String scheduledStepId,
        int slotIndex,
        String functionalityClassFqn,
        String functionalityClassSimpleName,
        String runtimeStepName,
        int assignedBit) {
    public static FaultVectorFault from(FaultVectorBoundaryContext context) {
        return new FaultVectorFault(
                context.scenarioExecutionId(),
                context.scenarioPlanId(),
                context.sagaInstanceId(),
                context.scheduledStepId(),
                context.slotIndex(),
                context.functionalityClassFqn(),
                context.functionalityClassSimpleName(),
                context.runtimeStepName(),
                context.assignedBit());
    }
}
