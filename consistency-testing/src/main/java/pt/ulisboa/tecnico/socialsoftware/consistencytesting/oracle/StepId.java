package pt.ulisboa.tecnico.socialsoftware.consistencytesting.oracle;

import org.jspecify.annotations.Nullable;

import pt.ulisboa.tecnico.socialsoftware.consistencytesting.utils.StringUtils;

public final class StepId {
    private static final String ID_SEPARATOR = "::";
    private static final String ID_CONNECTOR = "-";

    /**
     * Singleton of the synthetic step that represents the entire initial state
     * setup.
     */
    private static final StepId INITIAL_STATE_SETUP_STEP = new StepId(
            FunctionalityId.forInitialStateSetupFunctionality(), "step");

    private final FunctionalityId functionalityId;
    private final String id;

    private StepId(FunctionalityId functionalityId, String id) {
        this.functionalityId = functionalityId;
        this.id = functionalityId.toString() + ID_SEPARATOR + id;
    }

    public static StepId forFunctionalityStep(FunctionalityId functionalityId, String stepName) {
        return new StepId(functionalityId, stepName);
    }

    /**
     * @param functionalityId the identifier of the functionality this step belongs
     *                        to
     * @param zeroBasedIndex  the 0-based index representing the order in which the
     *                        step was registered (e.g., 0 for the 1st step, 1 for
     *                        the 2nd step)
     * @return the corresponding {@link StepId}
     * @throws IllegalArgumentException if {@code zeroBasedIndex} is negative
     */
    public static StepId forCompensationStep(FunctionalityId functionalityId, int zeroBasedIndex) {
        // ? TODO could it be possible to have "originalStepId-compensation" instead of
        // ? simple "1st-registeredCompensationStep" which is less descriptive?
        // ? Careful that since not all steps register compensations, so its not a
        // ? simple 1-to-1 relation between compensation steps
        // ? and the original function steps in order
        return new StepId(
                functionalityId,
                StringUtils.ordinal(zeroBasedIndex + 1) + ID_CONNECTOR + "registeredCompensationStep");
    }

    public static StepId forCommitStep(FunctionalityId functionalityId) {
        return new StepId(functionalityId, "commitStep");
    }

    public static StepId forAbortStep(FunctionalityId functionalityId) {
        return new StepId(functionalityId, "abortStep");
    }

    public static StepId forEventHandlerStep(FunctionalityId eventHandlerFunctionalityId) {
        return new StepId(eventHandlerFunctionalityId, "handlerStep");
    }

    /** The synthetic step that represents the entire initial state setup. */
    public static StepId forInitialStateSetupStep() {
        return INITIAL_STATE_SETUP_STEP;
    }

    public FunctionalityId getFunctionalityId() {
        return functionalityId;
    }

    @Override
    public boolean equals(@Nullable Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof StepId other)) {
            return false;
        }
        return id.equals(other.id);
    }

    @Override
    public int hashCode() {
        return id.hashCode();
    }

    @Override
    public String toString() {
        return id;
    }
}
