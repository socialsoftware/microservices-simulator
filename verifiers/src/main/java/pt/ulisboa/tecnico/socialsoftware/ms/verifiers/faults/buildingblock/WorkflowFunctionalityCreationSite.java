package pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.buildingblock;

import java.util.List;

public record WorkflowFunctionalityCreationSite(
        String classFqn,
        String methodName,
        String sagaClassFqn,
        List<WorkflowCreationArgumentSource> argumentSources) {

    public WorkflowFunctionalityCreationSite(String classFqn, String methodName, String sagaClassFqn) {
        this(classFqn, methodName, sagaClassFqn, List.of());
    }

    public WorkflowFunctionalityCreationSite {
        argumentSources = argumentSources == null ? List.of() : List.copyOf(argumentSources);
    }
}
