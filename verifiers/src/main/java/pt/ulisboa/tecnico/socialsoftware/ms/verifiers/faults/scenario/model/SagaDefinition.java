package pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.scenario.model;

import java.util.List;

public record SagaDefinition(
        String sagaFqn,
        List<StepDefinition> steps,
        List<String> warnings) {

    public SagaDefinition {
        sagaFqn = normalize(sagaFqn);
        steps = steps == null ? List.of() : List.copyOf(steps);
        warnings = warnings == null ? List.of() : List.copyOf(warnings);
    }

    private static String normalize(String value) {
        return value == null || value.isBlank() ? null : value;
    }
}
