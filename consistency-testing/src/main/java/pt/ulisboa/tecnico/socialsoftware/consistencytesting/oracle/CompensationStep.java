package pt.ulisboa.tecnico.socialsoftware.consistencytesting.oracle;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import pt.ulisboa.tecnico.socialsoftware.ms.coordination.WorkflowFunctionality;
import pt.ulisboa.tecnico.socialsoftware.ms.transaction.sagas.unitOfWork.SagaUnitOfWork;
import pt.ulisboa.tecnico.socialsoftware.ms.transaction.unitOfWork.UnitOfWork;

final class CompensationStep implements OracleStep {

    private final StepId id;
    private final FunctionalityId functionalityId;
    private final SagaUnitOfWork uow;
    private final String compensatedStepName;
    private final Set<StepId> dependencies;

    private CompensationStep(
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

    static List<CompensationStep> from(FunctionalityId functionalityId, WorkflowFunctionality functionality) {
        UnitOfWork uow = functionality.getWorkflow().getUnitOfWork();
        if (!(uow instanceof SagaUnitOfWork sagaUow)) {
            throw new IllegalArgumentException(
                    "Cannot create compensation steps for a workflow with a unit of work of type %s expect type was %s"
                            .formatted(uow.getClass(), SagaUnitOfWork.class));
        }

        // Mirrors how the simulator aborts: it walks the executed steps in reverse and
        // compensates each one that registered a compensation.
        // Steps that failed mid-execution are recorded as executed but register no
        // compensation, so they are skipped here too.
        Set<String> registeredCompensationStepNames = Set.copyOf(sagaUow.getRegisteredCompensationStepNames());

        List<CompensationStep> compensationSteps = new ArrayList<>();
        Set<StepId> previousStepsIds = new HashSet<>();

        for (String stepName : sagaUow.getExecutedSteps().reversed()) {
            if (!registeredCompensationStepNames.contains(stepName)) {
                continue;
            }

            var newCompensationStep = new CompensationStep(
                    functionalityId, sagaUow, stepName, previousStepsIds);

            compensationSteps.add(newCompensationStep);
            previousStepsIds.add(newCompensationStep.getId());
        }

        return compensationSteps;
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
