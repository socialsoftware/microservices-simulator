package pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.dynamic.model;

public record MatchedTestExecution(
        String testClassFqn,
        String testMethodName,
        String testDisplayName,
        String testUniqueId,
        String evidencePath,
        String testRunStatus) {
}
