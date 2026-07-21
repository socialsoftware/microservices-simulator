package pt.ulisboa.tecnico.socialsoftware.ms.coordination;

public record WorkflowStepRecoveryResult(
        String sourceStepName,
        boolean explicitCompensationExecuted,
        boolean implicitRollbackExecuted) {
}
