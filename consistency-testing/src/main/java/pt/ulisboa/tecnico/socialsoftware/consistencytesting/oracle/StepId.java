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
            FunctionalityId.forInitialStateSetupFunctionality(), "step", StepKind.FUNCTIONALITY);

    private final FunctionalityId functionalityId;
    private final String id;
    /** Identity used for cross-run behavioral comparison. */
    private final String behavioralIdentity;
    private final StepKind stepKind;
    private final boolean isIdentityStableAcrossRuns;

    private StepId(
            FunctionalityId functionalityId, String id, StepKind stepKind, boolean isIdentityStableAcrossRuns) {
        this.functionalityId = functionalityId;
        this.id = functionalityId.toString() + ID_SEPARATOR + id;
        this.behavioralIdentity = functionalityId.behavioralIdentity() + ID_SEPARATOR + id;
        this.stepKind = stepKind;
        this.isIdentityStableAcrossRuns = isIdentityStableAcrossRuns;
    }

    private StepId(FunctionalityId functionalityId, String id, StepKind stepKind) {
        this(functionalityId, id, stepKind, true);
    }

    public static StepId forFunctionalityStep(FunctionalityId functionalityId, String stepName) {
        return new StepId(functionalityId, stepName, StepKind.FUNCTIONALITY);
    }

    /**
     * @param functionalityId the identifier of the functionality this step belongs
     *                        to
     * @param stepName        the name of the functionality step whose compensation
     *                        this step runs
     * @return the corresponding {@link StepId}
     */
    public static StepId forCompensationStep(FunctionalityId functionalityId, String stepName) {
        return new StepId(functionalityId, stepName + ID_CONNECTOR + "compensation", StepKind.COMPENSATION);
    }

    public static StepId forCommitStep(FunctionalityId functionalityId) {
        return new StepId(functionalityId, "commitStep", StepKind.COMMIT);
    }

    /**
     * @param functionalityId the identifier of the functionality this step belongs
     *                        to
     * @param stepName        the name of the functionality step this step aborts
     * @return the corresponding {@link StepId}
     */
    public static StepId forAbortStep(FunctionalityId functionalityId, String stepName) {
        return new StepId(functionalityId, stepName + ID_CONNECTOR + "abort", StepKind.ABORT);
    }

    /**
     * Returns a StepId for an event handler step.
     * The identity of this step will not be considered stable across runs.
     */
    public static StepId forEventHandlerStep(FunctionalityId eventHandlerFunctionalityId) {
        return new StepId(eventHandlerFunctionalityId, "handlerStep", StepKind.EVENT_HANDLER, false);
    }

    /** The synthetic step that represents the entire initial state setup. */
    public static StepId forInitialStateSetupStep() {
        return INITIAL_STATE_SETUP_STEP;
    }

    public FunctionalityId getFunctionalityId() {
        return functionalityId;
    }

    /**
     * Whether this ID is expected to identify the same logical step after the
     * database is recreated for another run.
     * <p>
     * This says nothing about whether the step will materialize or execute in that
     * run. It only describes identity stability when the same logical step does
     * materialize.
     */
    public boolean isIdentityStableAcrossRuns() {
        return isIdentityStableAcrossRuns;
    }

    /**
     * Logical identity used only for cross-run behavioral observations. Unlike
     * {@link #toString()}, event-delivery identities omit database-generated event
     * and aggregate IDs. It must never be used to schedule or address a concrete
     * step: distinct deliveries can intentionally share this identity.
     */
    String behavioralIdentity() {
        return behavioralIdentity;
    }

    StepKind stepKind() {
        return stepKind;
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
