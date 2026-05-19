package pt.ulisboa.tecnico.socialsoftware.consistencytesting.oracle;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import pt.ulisboa.tecnico.socialsoftware.consistencytesting.utils.FunctionalityUtils;
import pt.ulisboa.tecnico.socialsoftware.ms.coordination.FlowStep;
import pt.ulisboa.tecnico.socialsoftware.ms.coordination.Step;
import pt.ulisboa.tecnico.socialsoftware.ms.coordination.Workflow;
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
        this.unitOfWork = uow; // continues the work on the originalFunc's unit of work

        Set<FlowStep> workflowCompensationSteps = buildCompensationStepsFromRegisteredCompensations(originalFunc);

        buildWorkflow(workflowCompensationSteps);
    }

    private static Set<FlowStep> buildCompensationStepsFromRegisteredCompensations(WorkflowFunctionality func) {
        Workflow workflow = func.getWorkflow();
        if (!(workflow.getUnitOfWork() instanceof SagaUnitOfWork uow)) {
            throw new IllegalArgumentException(
                    "Cannot retrive compensation plan from a unit of work of type %s expected type was %s"
                            .formatted(workflow.getUnitOfWork().getClass(), SagaUnitOfWork.class));
        }

        Set<FlowStep> compensationSteps = new HashSet<>();

        List<Runnable> registeredCompensations = uow.getRegisteredCompensations();
        for (Runnable compensation : registeredCompensations.reversed()) {
            // compensation index goes from highest index to 0 (representing the order in
            // which the compensations are being created)
            int compensationIndex = (registeredCompensations.size() - 1) - compensationSteps.size();
            String compensationStepName = FunctionalityUtils.getCompensationStepName(compensationIndex);

            // TODO do steps need to depend on all the previous or just the last?
            var dependencies = new ArrayList<FlowStep>(compensationSteps);

            // using Step instead of SagaStep to guarantee the assumption that compensation
            // steps cannot have compensations themselves
            var compensationStep = new Step(compensationStepName, compensation, dependencies);
            compensationSteps.add(compensationStep);
        }

        return compensationSteps;
    }

    public void buildWorkflow(Set<FlowStep> compensationSteps) {
        this.workflow = new SagaWorkflow(this, unitOfWorkService, unitOfWork);
        for (FlowStep step : compensationSteps) {
            this.workflow.addStep(step);
        }
    }
}
