package pt.ulisboa.tecnico.socialsoftware.ms.coordination;

public final class WorkflowStepRecoveryException extends RuntimeException {
    private final WorkflowStepRecoveryResult completedRecovery;
    private final String failedRecoveryKind;

    public WorkflowStepRecoveryException(WorkflowStepRecoveryResult completedRecovery,
                                         String failedRecoveryKind,
                                         Throwable cause) {
        super(cause);
        this.completedRecovery = completedRecovery;
        this.failedRecoveryKind = failedRecoveryKind;
    }

    public WorkflowStepRecoveryResult completedRecovery() {
        return completedRecovery;
    }

    public String failedRecoveryKind() {
        return failedRecoveryKind;
    }
}
