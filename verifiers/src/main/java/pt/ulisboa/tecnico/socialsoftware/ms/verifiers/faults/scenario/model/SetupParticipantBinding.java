package pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.scenario.model;

import java.util.List;

public record SetupParticipantBinding(
        String inputVariantId,
        int argumentIndex,
        String expectedTypeFqn,
        SetupValueRecipe value,
        List<String> blockers) {

    public SetupParticipantBinding {
        blockers = blockers == null ? List.of() : List.copyOf(blockers);
    }
}
