package pt.ulisboa.tecnico.socialsoftware.consistencytesting.oracle;

public enum TestStatus {
    INTERNAL_SYSTEM_EXCEPTION,
    CRITICAL_STEP_FAILURE,
    EXECUTION_LIMIT_EXCEEDED,
    INTERDEPENDENCY_RESOLUTION_FAILED,
    INTER_INVARIANT_VIOLATION

    // TODO "UNRECOVERABLE_INTERINVARIANT_VIOLATION" OR "INCONSISTENT_STATE"

    // TODO possible future status
    // INVARIANT_VIOLATION,
    // SEMANTIC_LOCKS,
    // COMPENSATED_ACTIONS,
    // DEADLOCK, this could exist but would require some deep logic to understand
    //              that the executed steps are showing a cyclic execution of their compensating
    //              actions (possibly due to 2+ sagas deadlocking each other with bad semantic locks)
}