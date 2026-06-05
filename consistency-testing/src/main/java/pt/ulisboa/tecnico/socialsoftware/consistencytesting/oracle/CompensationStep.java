package pt.ulisboa.tecnico.socialsoftware.consistencytesting.oracle;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

import pt.ulisboa.tecnico.socialsoftware.ms.coordination.WorkflowFunctionality;
import pt.ulisboa.tecnico.socialsoftware.ms.transaction.sagas.unitOfWork.SagaUnitOfWork;
import pt.ulisboa.tecnico.socialsoftware.ms.transaction.unitOfWork.UnitOfWork;

final class CompensationStep implements OracleStep {

    private final StepId id;
    private final FunctionalityId functionalityId;
    private final Runnable compensationAction;
    private final Set<StepId> dependencies;

    private CompensationStep(
            FunctionalityId functionalityId,
            WorkflowFunctionality functionality,
            int compensationIndex,
            Runnable compensationAction,
            Set<StepId> dependencies) {

        id = StepId.forCompensationStep(functionalityId, compensationIndex);
        this.functionalityId = functionalityId;
        this.compensationAction = compensationAction;
        this.dependencies = Set.copyOf(dependencies);
    }

    static List<CompensationStep> from(FunctionalityId functionalityId, WorkflowFunctionality functionality) {
        UnitOfWork uow = functionality.getWorkflow().getUnitOfWork();
        if (!(uow instanceof SagaUnitOfWork sagaUow)) {
            throw new IllegalArgumentException(
                    "Cannot create compensation steps for a workflow with a unit of work of type %s expect type was %s"
                            .formatted(uow.getClass(), SagaUnitOfWork.class));
        }

        List<CompensationStep> compensationSteps = new ArrayList<>();
        Set<StepId> previousStepsIds = new HashSet<>();
        List<Runnable> registeredCompensations = sagaUow.getRegisteredCompensations();

        int i = registeredCompensations.size() - 1;
        for (Runnable action : registeredCompensations.reversed()) {
            // compensationIndex goes from highest index to 0 (representing the order in
            // which the compensations are being created)
            int compensationIndex = i--;
            Objects.requireNonNull(action,
                    () -> "Compensation action cannot be null for compensation index %d at functionality %s"
                            .formatted(compensationIndex, functionalityId));

            var newCompensationStep = new CompensationStep(
                    functionalityId, functionality, compensationIndex, action, previousStepsIds);

            compensationSteps.add(newCompensationStep);
            previousStepsIds.add(newCompensationStep.getId());
        }

        return compensationSteps;
    }

    @Override
    public void execute() {
        compensationAction.run();
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