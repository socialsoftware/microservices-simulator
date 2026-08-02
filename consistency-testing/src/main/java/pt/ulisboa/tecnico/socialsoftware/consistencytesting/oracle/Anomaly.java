package pt.ulisboa.tecnico.socialsoftware.consistencytesting.oracle;

/**
 * An isolation anomaly found by the {@link AnomalyAnalyzer} in a run's effect
 * sequence. Each variant carries the evidence that triggered it.
 */
public sealed interface Anomaly permits Anomaly.DirtyRead, Anomaly.NonRepeatableRead, Anomaly.WriteSkew {

    AnomalyType type();

    /** Human-readable one-liner for logs and reports. */
    String description();

    /**
     * A functionality step read a write made by a foreign functionality that later
     * compensated: the reader observed — and possibly acted on — data that was
     * subsequently undone (or that the writer at least attempted to undo).
     *
     * @param reader        the functionality step that performed the read
     * @param doomedWriter  the foreign functionality step whose write was read
     *                      and later compensated
     * @param aggregateType the type of the aggregate that was read
     */
    record DirtyRead(StepId reader, StepId doomedWriter, String aggregateType) implements Anomaly {

        @Override
        public AnomalyType type() {
            return AnomalyType.DIRTY_READ;
        }

        @Override
        public String description() {
            return "Step '%s' read a '%s' write made by '%s', whose functionality later compensated: the read observed doomed data"
                    .formatted(reader, aggregateType, doomedWriter);
        }
    }

    /**
     * Two functionality-step reads of the same aggregate, within the same
     * functionality, observed versions produced by different writers — the
     * second one by a foreign functionality. Whatever the first read validated
     * or extracted may no longer hold by the second read.
     *
     * @param functionality the functionality whose reads disagreed
     * @param firstRead     the earlier read step
     * @param secondRead    the later read step
     * @param firstWriter   the step whose write the first read observed
     * @param secondWriter  the foreign step whose write the second read observed
     * @param aggregateType the type of the aggregate that was read twice
     */
    record NonRepeatableRead(
            FunctionalityId functionality,
            StepId firstRead,
            StepId secondRead,
            StepId firstWriter,
            StepId secondWriter,
            String aggregateType) implements Anomaly {

        @Override
        public AnomalyType type() {
            return AnomalyType.NON_REPEATABLE_READ;
        }

        @Override
        public String description() {
            return ("Functionality '%s' read '%s' twice and saw different versions: "
                    + "'%s' read the write of '%s', but '%s' read the write of foreign '%s'")
                    .formatted(functionality, aggregateType, firstRead, firstWriter, secondRead, secondWriter);
        }
    }

    /**
     * Two committed functionalities read state the other one subsequently
     * invalidated without ever seeing each other's writes: each decided on a
     * snapshot the other invalidated, and both decisions committed.
     *
     * @param functionalityA              one functionality of the skewed pair
     * @param functionalityB              the other functionality of the pair
     * @param aggregateTypeOverwrittenByB type of the aggregate A read and B
     *                                    later modified
     * @param aggregateTypeOverwrittenByA type of the aggregate B read and A
     *                                    later modified
     */
    record WriteSkew(
            FunctionalityId functionalityA,
            FunctionalityId functionalityB,
            String aggregateTypeOverwrittenByB,
            String aggregateTypeOverwrittenByA) implements Anomaly {

        @Override
        public AnomalyType type() {
            return AnomalyType.WRITE_SKEW;
        }

        @Override
        public String description() {
            return ("Functionalities '%s' and '%s' both committed after reading state the other invalidated "
                    + "('%s' modified by the latter, '%s' modified by the former) without seeing each other's writes")
                    .formatted(functionalityA, functionalityB,
                            aggregateTypeOverwrittenByB, aggregateTypeOverwrittenByA);
        }
    }
}
