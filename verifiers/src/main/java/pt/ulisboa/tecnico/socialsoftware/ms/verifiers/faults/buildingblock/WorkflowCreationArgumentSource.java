package pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.buildingblock;

public record WorkflowCreationArgumentSource(
        int argumentIndex,
        WorkflowCreationArgumentSourceKind kind,
        Integer parameterIndex,
        String name,
        String text) {
}
