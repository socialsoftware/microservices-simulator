package pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.executor;

public final class ScenarioReportWriteException extends RuntimeException {
    private final ScenarioExecutionReport report;

    public ScenarioReportWriteException(ScenarioExecutionReport report, Throwable cause) {
        super("Failed to write scenario execution report", cause);
        this.report = report;
    }

    public ScenarioExecutionReport report() {
        return report;
    }
}
