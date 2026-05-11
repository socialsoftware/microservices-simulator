package pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.dynamic.model;

import java.util.List;

public record ObservedCommand(
        String sagaFqn,
        String stepName,
        String commandType,
        String commandFqn,
        String serviceName,
        String rootAggregateId,
        List<String> sourceEventIds) {
    public ObservedCommand {
        sourceEventIds = sourceEventIds == null ? List.of() : List.copyOf(sourceEventIds);
    }
}
