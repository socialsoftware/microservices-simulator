package pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.dynamic.model;

import java.util.List;

public record ObservedStep(String sagaFqn, String functionalityName, String stepName, List<String> eventKinds, List<String> outcomes) {
    public ObservedStep {
        eventKinds = eventKinds == null ? List.of() : List.copyOf(eventKinds);
        outcomes = outcomes == null ? List.of() : List.copyOf(outcomes);
    }
}
