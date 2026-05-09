package pt.ulisboa.tecnico.socialsoftware.consistencytesting.oracle;

import java.util.Set;

import pt.ulisboa.tecnico.socialsoftware.consistencytesting.utils.FunctionalityUtils;
import pt.ulisboa.tecnico.socialsoftware.ms.coordination.FlowStep;
import pt.ulisboa.tecnico.socialsoftware.ms.coordination.WorkflowFunctionality;
import pt.ulisboa.tecnico.socialsoftware.ms.transaction.sagas.unitOfWork.SagaUnitOfWork;
import pt.ulisboa.tecnico.socialsoftware.ms.transaction.sagas.unitOfWork.SagaUnitOfWorkService;
import pt.ulisboa.tecnico.socialsoftware.ms.transaction.sagas.workflow.SagaWorkflow;

public class CompensationFunctionality extends WorkflowFunctionality {
    private final SagaUnitOfWorkService unitOfWorkService;
    private final SagaUnitOfWork unitOfWork;

    public CompensationFunctionality(WorkflowFunctionality originalFunc, SagaUnitOfWorkService unitOfWorkService) {
        this.unitOfWorkService = unitOfWorkService;

        if (!(originalFunc.getWorkflow().getUnitOfWork() instanceof SagaUnitOfWork uow)) {
            throw new IllegalArgumentException(
                    "Cannot create compensation functionality for a workflow with a unit of work of type %s expect type was %s"
                            .formatted(originalFunc.getWorkflow().getUnitOfWork().getClass(), SagaUnitOfWork.class));
        }
        // continues the work on the originalFunc's unit of work, as it would normally
        this.unitOfWork = uow;

        Set<FlowStep> workflowCompensationSteps = FunctionalityUtils.getCurrentCompensationSteps(originalFunc);
        buildWorkflow(workflowCompensationSteps);
    }

    public void buildWorkflow(Set<FlowStep> compensationSteps) {
        this.workflow = new SagaWorkflow(this, unitOfWorkService, unitOfWork);
        for (FlowStep step : compensationSteps) {
            this.workflow.addStep(step);
        }
    }
}
