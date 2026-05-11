package pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.dynamic.model;

import java.util.List;

public record ObservedAggregateAccess(
        String sagaFqn,
        String stepName,
        String accessMode,
        String aggregateType,
        String aggregateId,
        String sourceMethod,
        List<String> sourceEventIds) {
    public ObservedAggregateAccess {
        sourceEventIds = sourceEventIds == null ? List.of() : List.copyOf(sourceEventIds);
    }
}
