package pt.ulisboa.tecnico.socialsoftware.consistencytesting.oracle;

import java.util.Set;

import pt.ulisboa.tecnico.socialsoftware.ms.transaction.sagas.unitOfWork.SagaUnitOfWork;
import pt.ulisboa.tecnico.socialsoftware.ms.transaction.sagas.unitOfWork.SagaUnitOfWorkService;

/**
 * Runs the abort logic for a single functionality step, which is the second
 * half of what the system does for that step while aborting. The first half is
 * handled by the corresponding {@link CompensationStep}.
 */
final class AbortStep implements OracleStep {

    private final StepId id;
    private final FunctionalityId functionalityId;
    private final SagaUnitOfWorkService uowService;
    private final SagaUnitOfWork uow;
    private final String abortedStepName;
    private final Set<StepId> dependencies;

    AbortStep(
            FunctionalityId functionalityId,
            SagaUnitOfWork uow,
            SagaUnitOfWorkService uowService,
            String abortedStepName,
            Set<StepId> dependencies) {

        id = StepId.forAbortStep(functionalityId, abortedStepName);
        this.functionalityId = functionalityId;
        this.uowService = uowService;
        this.uow = uow;
        this.abortedStepName = abortedStepName;
        this.dependencies = Set.copyOf(dependencies);
    }

    @Override
    public void execute() {
        // Same guard the simulator's own abort loop uses, so a step whose locks were
        // already reverted (by an earlier partial abort) is not reverted twice.
        if (uow.isStepAborted(abortedStepName)) {
            return;
        }

        uowService.sendAbortCommandsForStep(uow, abortedStepName);
        uow.setStepAborted(abortedStepName);
    }

    @Override
    public StepId getId() {
        return id;
    }

    @Override
    public FunctionalityId getFunctionalityId() {
        return functionalityId;
    }

    @Override
    public Set<StepId> getDependencies() {
        return dependencies;
    }
}
