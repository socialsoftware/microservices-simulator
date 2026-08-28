package pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.buildingblock;

import java.util.List;

public record WorkflowFunctionalityCreationSite(
        String classFqn,
        String methodName,
        String methodKey,
        List<String> parameterTypeFqns,
        String declaredResultTypeFqn,
        boolean voidResult,
        String sagaClassFqn,
        List<WorkflowCreationArgumentSource> argumentSources) {

    public WorkflowFunctionalityCreationSite(String classFqn, String methodName, String sagaClassFqn) {
        this(classFqn, methodName, null, List.of(), null, false, sagaClassFqn, List.of());
    }

    public WorkflowFunctionalityCreationSite {
        parameterTypeFqns = parameterTypeFqns == null ? List.of() : List.copyOf(parameterTypeFqns);
        argumentSources = argumentSources == null ? List.of() : List.copyOf(argumentSources);
    }
}
