package pt.ulisboa.tecnico.socialsoftware.consistencytesting.oracle;

public enum TestStatus {
    INTERNAL_EXCEPTION,
    RUN_LIMIT_EXCEEDED,
    SCHEDULE_REJECTED

    // TODO possible future status
    // HEALTHY, for now we are deeming this as the absence of other status
    // CRASHED, this should be reserved for when the whole system crashed, and needs
    // rebooting
    // INVARIANT_VIOLATION,
    // SEMANTIC_LOCKS,
    // COMPENSATED_ACTIONS,
    // DEADLOCK, this could exist but would require some deep logic to understand
    //              that the executed steps are showing a cyclic execution of their compensating
    //              actions (possibly due to 2+ sagas deadlocking each other with bad semantic locks)
}