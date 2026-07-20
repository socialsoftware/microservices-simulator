package pt.ulisboa.tecnico.socialsoftware.ms.coordination;

public record WorkflowStepExecutionResult(boolean completed, Throwable failure) {
    public static WorkflowStepExecutionResult success() {
        return new WorkflowStepExecutionResult(true, null);
    }

    public static WorkflowStepExecutionResult failed(Throwable failure) {
        return new WorkflowStepExecutionResult(false, failure);
    }
}
