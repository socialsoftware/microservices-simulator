package pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.scenario.model;

import java.util.List;

public record StepFootprint(
        AggregateKey aggregateKey,
        AccessMode accessMode,
        List<String> warnings) {

    public StepFootprint {
        accessMode = accessMode == null ? AccessMode.WRITE : accessMode;
        warnings = warnings == null ? List.of() : List.copyOf(warnings);
    }
}
