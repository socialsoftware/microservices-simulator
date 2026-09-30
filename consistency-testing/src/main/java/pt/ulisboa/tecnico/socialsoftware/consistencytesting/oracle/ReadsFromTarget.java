package pt.ulisboa.tecnico.socialsoftware.consistencytesting.oracle;

import java.util.Objects;

/** A cross-step aggregate observation that one run will try to realize. */
public record ReadsFromTarget(StepId writer, StepId reader, String aggregateType) {

    public ReadsFromTarget {
        Objects.requireNonNull(writer);
        Objects.requireNonNull(reader);
        Objects.requireNonNull(aggregateType);
        if (writer.equals(reader)) {
            throw new IllegalArgumentException("writer and reader must differ");
        }
    }

    public boolean observedIn(TestResult result) {
        return result.readsFromRelations().contains(new ReadsFromRelation(reader, writer, aggregateType));
    }
}
