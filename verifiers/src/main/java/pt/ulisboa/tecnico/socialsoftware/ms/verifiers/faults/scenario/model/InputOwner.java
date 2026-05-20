package pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.scenario.model;

public record InputOwner(String testClassFqn, String testMethodName) {

    public InputOwner {
        testClassFqn = normalize(testClassFqn);
        testMethodName = normalize(testMethodName);
    }

    public boolean isComplete() {
        return testClassFqn != null && testMethodName != null;
    }

    private static String normalize(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
