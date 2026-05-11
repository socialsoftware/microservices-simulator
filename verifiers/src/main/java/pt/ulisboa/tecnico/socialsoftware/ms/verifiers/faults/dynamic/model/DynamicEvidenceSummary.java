package pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.dynamic.model;

import java.util.List;

public record DynamicEvidenceSummary(
        DynamicEvidenceJoinStatus joinStatus,
        List<String> matchedInputVariantIds,
        List<MatchedTestExecution> matchedTestExecutions,
        List<ObservedStep> observedSteps,
        List<ObservedAggregateAccess> observedAggregateAccesses,
        List<ObservedCommand> observedCommands,
        List<String> warnings) {
    public DynamicEvidenceSummary {
        joinStatus = joinStatus == null ? DynamicEvidenceJoinStatus.NOT_COVERED : joinStatus;
        matchedInputVariantIds = matchedInputVariantIds == null ? List.of() : List.copyOf(matchedInputVariantIds);
        matchedTestExecutions = matchedTestExecutions == null ? List.of() : List.copyOf(matchedTestExecutions);
        observedSteps = observedSteps == null ? List.of() : List.copyOf(observedSteps);
        observedAggregateAccesses = observedAggregateAccesses == null ? List.of() : List.copyOf(observedAggregateAccesses);
        observedCommands = observedCommands == null ? List.of() : List.copyOf(observedCommands);
        warnings = warnings == null ? List.of() : List.copyOf(warnings);
    }
}
