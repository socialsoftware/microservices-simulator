package pt.ulisboa.tecnico.socialsoftware.consistencytesting.oracle;

/** The kind of an {@link OracleStep}. */
public enum StepKind {
    FUNCTIONALITY,
    COMPENSATION,
    COMMIT,
    ABORT,
    EVENT_HANDLER;

    static StepKind of(OracleStep step) {
        return switch (step) {
            case FunctionalityStep s -> FUNCTIONALITY;
            case CompensationStep s -> COMPENSATION;
            case CommitStep s -> COMMIT;
            case AbortStep s -> ABORT;
            case EventHandlerStep s -> EVENT_HANDLER;
        };
    }
}
