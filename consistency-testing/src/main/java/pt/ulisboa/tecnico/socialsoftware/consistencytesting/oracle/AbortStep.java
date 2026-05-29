package pt.ulisboa.tecnico.socialsoftware.consistencytesting.oracle;

import java.util.Set;

import pt.ulisboa.tecnico.socialsoftware.ms.coordination.WorkflowFunctionality;
import pt.ulisboa.tecnico.socialsoftware.ms.transaction.sagas.unitOfWork.SagaUnitOfWork;
import pt.ulisboa.tecnico.socialsoftware.ms.transaction.sagas.unitOfWork.SagaUnitOfWorkService;
import pt.ulisboa.tecnico.socialsoftware.ms.transaction.unitOfWork.UnitOfWork;

public class AbortStep implements OracleStep {

    private final StepId id;
    private final FunctionalityId functionalityId;
    private final SagaUnitOfWorkService uowService;
    private final SagaUnitOfWork uow;
    private final Set<StepId> dependencies;

    AbortStep(
            FunctionalityId functionalityId,
            WorkflowFunctionality functionality,
            Set<StepId> dependencies,
            SagaUnitOfWorkService uowService) {

        UnitOfWork uow = functionality.getWorkflow().getUnitOfWork();
        if (!(uow instanceof SagaUnitOfWork sagaUow)) {
            throw new IllegalArgumentException(
                    "Cannot retrieve abort step from a unit of work of type %s expected type was %s"
                            .formatted(uow.getClass(), SagaUnitOfWork.class));
        }

        id = StepId.forAbortStep(functionalityId);
        this.functionalityId = functionalityId;
        this.uowService = uowService;
        this.uow = sagaUow;
        this.dependencies = Set.copyOf(dependencies);
    }

    @Override
    public void execute() {
        uowService.abort(uow);
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