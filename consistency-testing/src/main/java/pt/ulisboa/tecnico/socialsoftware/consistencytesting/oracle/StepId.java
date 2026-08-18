package pt.ulisboa.tecnico.socialsoftware.consistencytesting.oracle;

import org.jspecify.annotations.Nullable;

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
     * @param stepName        the name of the functionality step whose compensation
     *                        this step runs
     * @return the corresponding {@link StepId}
     */
    public static StepId forCompensationStep(FunctionalityId functionalityId, String stepName) {
        return new StepId(functionalityId, stepName + ID_CONNECTOR + "compensation");
    }

    public static StepId forCommitStep(FunctionalityId functionalityId) {
        return new StepId(functionalityId, "commitStep");
    }

    /**
     * @param functionalityId the identifier of the functionality this step belongs
     *                        to
     * @param stepName        the name of the functionality step this step aborts
     * @return the corresponding {@link StepId}
     */
    public static StepId forAbortStep(FunctionalityId functionalityId, String stepName) {
        return new StepId(functionalityId, stepName + ID_CONNECTOR + "abort");
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
