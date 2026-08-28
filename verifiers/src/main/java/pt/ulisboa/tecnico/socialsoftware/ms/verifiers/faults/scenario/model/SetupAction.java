package pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.scenario.model;

import java.util.List;

public record SetupAction(
        String actionId,
        int orderIndex,
        String sourceOccurrence,
        String methodKey,
        List<SetupArgument> arguments,
        String declaredResultTypeFqn,
        boolean voidResult,
        List<String> blockers) {

    public SetupAction {
        arguments = arguments == null ? List.of() : List.copyOf(arguments);
        blockers = blockers == null ? List.of() : List.copyOf(blockers);
    }
}
