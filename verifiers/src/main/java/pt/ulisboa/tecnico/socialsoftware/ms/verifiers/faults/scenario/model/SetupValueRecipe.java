package pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.scenario.model;

import java.util.List;

/** Closed persisted value language for source-derived setup. */
public record SetupValueRecipe(
        SetupValueKind kind,
        String declaredTypeFqn,
        String literalKind,
        Object literalValue,
        String targetTypeFqn,
        List<SetupValueRecipe> constructorArguments,
        List<SetupPropertyAssignment> assignments,
        List<SetupValueRecipe> elements,
        SetupValueRecipe receiver,
        String actionId,
        String propertyName,
        List<String> blockers) {

    public SetupValueRecipe {
        constructorArguments = constructorArguments == null ? List.of() : List.copyOf(constructorArguments);
        assignments = assignments == null ? List.of() : List.copyOf(assignments);
        elements = elements == null ? List.of() : List.copyOf(elements);
        blockers = blockers == null ? List.of() : List.copyOf(blockers);
    }

    public static SetupValueRecipe actionResult(String actionId, String declaredTypeFqn) {
        return new SetupValueRecipe(SetupValueKind.ACTION_RESULT, declaredTypeFqn, null, null,
                null, List.of(), List.of(), List.of(), null, actionId, null, List.of());
    }

    public static SetupValueRecipe actionProperty(String actionId, String propertyName, String declaredTypeFqn) {
        return new SetupValueRecipe(SetupValueKind.ACTION_RESULT_PROPERTY, declaredTypeFqn, null, null,
                null, List.of(), List.of(), List.of(), null, actionId, propertyName, List.of());
    }
}
