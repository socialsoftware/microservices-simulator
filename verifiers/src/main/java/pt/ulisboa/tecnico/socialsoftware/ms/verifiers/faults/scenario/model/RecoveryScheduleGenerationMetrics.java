package pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.scenario.model;

public record RecoveryScheduleGenerationMetrics(
        long countingStatesVisited,
        long representativeCandidatesConstructed,
        long materializedLeavesVisited) {
}
