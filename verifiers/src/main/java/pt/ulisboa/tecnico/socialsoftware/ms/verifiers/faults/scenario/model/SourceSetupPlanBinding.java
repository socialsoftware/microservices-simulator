package pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.scenario.model;

import java.util.List;

/** Connects one coherent observed fixture setup to every accepted input it can supply. */
public record SourceSetupPlanBinding(
        List<String> inputVariantIds,
        SetupPlan setupPlan) {

    public SourceSetupPlanBinding {
        inputVariantIds = inputVariantIds == null ? List.of() : inputVariantIds.stream()
                .filter(id -> id != null && !id.isBlank())
                .distinct()
                .sorted()
                .toList();
    }
}
