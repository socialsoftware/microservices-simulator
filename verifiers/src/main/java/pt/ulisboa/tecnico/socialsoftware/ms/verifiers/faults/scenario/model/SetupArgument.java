package pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.scenario.model;

import java.util.List;

public record SetupArgument(
        int index,
        String expectedTypeFqn,
        SetupValueRecipe value,
        List<String> blockers) {

    public SetupArgument {
        blockers = blockers == null ? List.of() : List.copyOf(blockers);
    }
}
