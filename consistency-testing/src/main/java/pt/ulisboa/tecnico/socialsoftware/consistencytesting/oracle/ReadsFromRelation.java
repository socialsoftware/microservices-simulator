package pt.ulisboa.tecnico.socialsoftware.consistencytesting.oracle;

/**
 * A reads-from relation observed during a run: {@code reader} read an aggregate
 * of {@code aggregateType} whose current value was produced by {@code writer}.
 * <p>
 * The writer is the step that last wrote that aggregate before the read; if the
 * aggregate was only ever written while building the initial state, the writer
 * is the {@link StepId#forInitialStateSetupStep()}.
 * <p>
 * The relation is keyed by {@code aggregateType} rather than aggregate id on
 * purpose: ids are reassigned every run, so type is what stays comparable
 * across runs and reports.
 */
public record ReadsFromRelation(StepId reader, StepId writer, String aggregateType) {
}
