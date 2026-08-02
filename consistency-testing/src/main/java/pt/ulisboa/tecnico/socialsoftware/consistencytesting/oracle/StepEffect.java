package pt.ulisboa.tecnico.socialsoftware.consistencytesting.oracle;

/**
 * One entry of a run's effect sequence: a single aggregate-level read or write,
 * attributed to the step that was executing when it happened ({@code stepId},
 * whose kind is {@code stepKind}), at position {@code sequenceNumber} in the
 * total order of effects observed during the run.
 * <p>
 * The {@code aggregateId} is what matches a read to the write it observed
 * within a run, while {@code aggregateType} is the stable identifier for humans
 * and reports (aggregate ids are reassigned every run, so they are not
 * comparable across runs).
 */
public record StepEffect(
        int sequenceNumber,
        StepId stepId,
        StepKind stepKind,
        EffectKind effectKind,
        Integer aggregateId,
        String aggregateType) {

    public enum EffectKind {
        READ, WRITE
    }

    static StepEffect of(int sequenceNumber, StepId stepId, StepKind stepKind, Effect effect) {
        EffectKind effectKind = switch (effect) {
            case Effect.Read read -> EffectKind.READ;
            case Effect.Write write -> EffectKind.WRITE;
        };
        return new StepEffect(
                sequenceNumber, stepId, stepKind, effectKind, effect.aggregateId(), effect.aggregateType());
    }

    public boolean isRead() {
        return effectKind == EffectKind.READ;
    }

    public boolean isWrite() {
        return effectKind == EffectKind.WRITE;
    }

    public FunctionalityId functionalityId() {
        return stepId.getFunctionalityId();
    }
}
