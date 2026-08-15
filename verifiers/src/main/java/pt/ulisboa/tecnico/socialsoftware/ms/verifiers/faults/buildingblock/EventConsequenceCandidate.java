package pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.buildingblock;

import java.util.List;

public record EventConsequenceCandidate(
        String triggerSagaFqn,
        String triggerStepKey,
        EventEmissionSiteFact emissionSite,
        EventDrivenFunctionalityInvocation selectedConsumerRoute,
        List<String> diagnostics) {

    public EventConsequenceCandidate {
        diagnostics = diagnostics == null ? List.of() : List.copyOf(diagnostics);
    }
}
