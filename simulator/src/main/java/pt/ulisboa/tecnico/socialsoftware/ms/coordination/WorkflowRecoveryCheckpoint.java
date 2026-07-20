package pt.ulisboa.tecnico.socialsoftware.ms.coordination;

public record WorkflowRecoveryCheckpoint(
        String sourceStepName,
        boolean explicitCompensationPending,
        boolean implicitRollbackPending) {
}
