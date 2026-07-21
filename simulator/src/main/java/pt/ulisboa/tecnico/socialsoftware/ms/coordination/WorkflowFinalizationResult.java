package pt.ulisboa.tecnico.socialsoftware.ms.coordination;

public record WorkflowFinalizationResult(boolean committed, Throwable failure) {
    public static WorkflowFinalizationResult success() {
        return new WorkflowFinalizationResult(true, null);
    }

    public static WorkflowFinalizationResult failed(Throwable failure) {
        return new WorkflowFinalizationResult(false, failure);
    }
}
