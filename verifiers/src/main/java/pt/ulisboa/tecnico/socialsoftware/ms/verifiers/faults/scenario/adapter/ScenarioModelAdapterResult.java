package pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.scenario.adapter;

import pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.scenario.model.InputVariant;
import pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.scenario.model.SagaDefinition;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public record ScenarioModelAdapterResult(
        List<SagaDefinition> sagaDefinitions,
        List<InputVariant> inputVariants,
        Map<String, Integer> counts,
        List<String> diagnostics) {

    public ScenarioModelAdapterResult {
        sagaDefinitions = sagaDefinitions == null ? List.of() : List.copyOf(sagaDefinitions);
        inputVariants = inputVariants == null ? List.of() : List.copyOf(inputVariants);
        counts = counts == null ? Map.of() : Collections.unmodifiableMap(new LinkedHashMap<>(counts));
        diagnostics = diagnostics == null ? List.of() : List.copyOf(diagnostics);
    }
}
