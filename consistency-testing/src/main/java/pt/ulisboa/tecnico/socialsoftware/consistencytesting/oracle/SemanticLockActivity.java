package pt.ulisboa.tecnico.socialsoftware.consistencytesting.oracle;

import java.util.Objects;

/** A semantic-lock acquisition observed while executing one oracle step. */
public record SemanticLockActivity(
        StepId stepId,
        SemanticLockId semanticLock,
        Integer aggregateId,
        Outcome outcome) {

    public enum Outcome {
        ACQUIRED,
        SKIPPED,
        /** The operation was blocked by a semantic lock held by another saga. */
        REJECTED
    }

    public SemanticLockActivity {
        Objects.requireNonNull(stepId);
        Objects.requireNonNull(semanticLock);
        Objects.requireNonNull(aggregateId);
        Objects.requireNonNull(outcome);
    }
}
