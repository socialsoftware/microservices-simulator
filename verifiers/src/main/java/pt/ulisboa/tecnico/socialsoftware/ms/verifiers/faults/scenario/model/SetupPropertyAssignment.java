package pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.scenario.model;

import java.util.List;

public record SetupPropertyAssignment(
        int orderIndex,
        String propertyName,
        SetupValueRecipe value,
        List<String> blockers) {

    public SetupPropertyAssignment {
        blockers = blockers == null ? List.of() : List.copyOf(blockers);
    }
}
