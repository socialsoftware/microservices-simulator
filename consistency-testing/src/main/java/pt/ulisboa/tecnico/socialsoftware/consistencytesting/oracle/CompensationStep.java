package pt.ulisboa.tecnico.socialsoftware.consistencytesting.oracle;

import java.util.Set;

import pt.ulisboa.tecnico.socialsoftware.ms.transaction.sagas.unitOfWork.SagaUnitOfWork;

/**
 * Runs the compensation registered by a single functionality step, which is the
 * first half of what the system does for that step while aborting. The second
 * half is handled by the corresponding {@link AbortStep}.
 */
final class CompensationStep implements OracleStep {

    private final StepId id;
    private final FunctionalityId functionalityId;
    private final SagaUnitOfWork uow;
    private final String compensatedStepName;
    private final Set<StepId> dependencies;

    CompensationStep(
            FunctionalityId functionalityId,
            SagaUnitOfWork uow,
            String compensatedStepName,
            Set<StepId> dependencies) {

        id = StepId.forCompensationStep(functionalityId, compensatedStepName);
        this.functionalityId = functionalityId;
        this.uow = uow;
        this.compensatedStepName = compensatedStepName;
        this.dependencies = Set.copyOf(dependencies);
    }

    @Override
    public void execute() {
        uow.compensateStep(compensatedStepName);
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
