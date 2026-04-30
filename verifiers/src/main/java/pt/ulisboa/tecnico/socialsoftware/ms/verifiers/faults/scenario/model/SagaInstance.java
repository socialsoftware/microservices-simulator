package pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.scenario.model;

import java.util.List;

public record SagaInstance(
        String deterministicId,
        String sagaFqn,
        String inputVariantId,
        List<String> warnings) {

    public SagaInstance {
        deterministicId = normalize(deterministicId);
        sagaFqn = normalize(sagaFqn);
        inputVariantId = normalize(inputVariantId);
        warnings = warnings == null ? List.of() : List.copyOf(warnings);
    }

    private static String normalize(String value) {
        return value == null || value.isBlank() ? null : value;
    }
}
