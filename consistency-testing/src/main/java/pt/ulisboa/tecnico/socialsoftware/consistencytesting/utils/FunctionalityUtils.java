package pt.ulisboa.tecnico.socialsoftware.consistencytesting.utils;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

import pt.ulisboa.tecnico.socialsoftware.ms.coordination.ExecutionPlan;
import pt.ulisboa.tecnico.socialsoftware.ms.coordination.FlowStep;
import pt.ulisboa.tecnico.socialsoftware.ms.coordination.Step;
import pt.ulisboa.tecnico.socialsoftware.ms.coordination.Workflow;
import pt.ulisboa.tecnico.socialsoftware.ms.coordination.WorkflowFunctionality;
import pt.ulisboa.tecnico.socialsoftware.ms.transaction.sagas.unitOfWork.SagaUnitOfWork;

public class FunctionalityUtils {

    public static List<FlowStep> getSteps(WorkflowFunctionality func) {
        Workflow workflow = func.getWorkflow();
        ExecutionPlan plan = workflow.getOrCreateExecutionPlan();
        return Objects.requireNonNull(plan.getPlan());
    }

    public static Set<FlowStep> getCurrentCompensationSteps(WorkflowFunctionality func) {
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
            String compensationStepName = getCompensationStepName(compensationIndex);

            // TODO do steps need to depend on all the previous or just the last?
            // should depend on all the previous, instead of just depending on the single
            // previous step
            var dependencies = new ArrayList<FlowStep>(compensationSteps);

            // using Step instead of SagaStep to guarantee the assumption that compensation
            // steps cannot have compensations themselves
            var compensationStep = new Step(compensationStepName, compensation, dependencies);
            compensationSteps.add(compensationStep);
        }

        return compensationSteps;
    }

    /**
     * Builds the default name for a compensation step.
     *
     * @param compensationIndex zero-based compensation step index, where 0
     *                          corresponds to the compensation of the first step, 1
     *                          to the compensation of the second step, and so on.
     * @return the generated compensation step name
     */
    public static String getCompensationStepName(int compensationIndex) {
        // TODO could be extended with original step or func name to be more descriptive
        return "compensate-step-" + compensationIndex;
    }
}
