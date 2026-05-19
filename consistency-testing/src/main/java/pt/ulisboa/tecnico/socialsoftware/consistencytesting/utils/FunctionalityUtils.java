package pt.ulisboa.tecnico.socialsoftware.consistencytesting.utils;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Stream;

import pt.ulisboa.tecnico.socialsoftware.consistencytesting.oracle.CompensationFunctionality;
import pt.ulisboa.tecnico.socialsoftware.ms.coordination.ExecutionPlan;
import pt.ulisboa.tecnico.socialsoftware.ms.coordination.FlowStep;
import pt.ulisboa.tecnico.socialsoftware.ms.coordination.Step;
import pt.ulisboa.tecnico.socialsoftware.ms.coordination.Workflow;
import pt.ulisboa.tecnico.socialsoftware.ms.coordination.WorkflowFunctionality;
import pt.ulisboa.tecnico.socialsoftware.ms.transaction.sagas.unitOfWork.SagaUnitOfWork;
import pt.ulisboa.tecnico.socialsoftware.ms.transaction.sagas.unitOfWork.SagaUnitOfWorkService;

public class FunctionalityUtils {

    /**
     * Returns an immutable list of the workflow steps for the given functionality,
     * in order according to the execution plan.
     *
     * @param func the workflow functionality
     * @return an immutable list of flow steps
     */
    public static List<FlowStep> getSteps(WorkflowFunctionality func) {
        Workflow workflow = func.getWorkflow();
        ExecutionPlan plan = workflow.getOrCreateExecutionPlan();
        ArrayList<FlowStep> funcSteps = Objects.requireNonNull(plan.getPlan());
        return List.copyOf(funcSteps);
    }

    public static FlowStep createCommitStep(WorkflowFunctionality func, SagaUnitOfWorkService uowService) {
        if (func instanceof CompensationFunctionality) {
            throw new IllegalArgumentException("Cannot create a commit step for a compensation functionality");
        }

        Workflow workflow = func.getWorkflow();
        if (!(workflow.getUnitOfWork() instanceof SagaUnitOfWork uow)) {
            throw new IllegalArgumentException(
                    "Cannot retrive commit step from a unit of work of type %s expected type was %s"
                            .formatted(workflow.getUnitOfWork().getClass(), SagaUnitOfWork.class));
        }

        // TODO should the commit step depend on all steps or just the last one?
        Runnable commitAction = () -> uowService.commit(uow);
        var stepDeps = new ArrayList<FlowStep>(getSteps(func));
        var commitStep = new Step(getCommitStepName(func), commitAction, stepDeps);

        // workflow needs the commit step so it can execute it with executeUntilStep
        workflow.addStep(commitStep);
        return commitStep;
    }

    public static FlowStep createAbortStep(
            CompensationFunctionality compensationFunc,
            SagaUnitOfWorkService uowService) {

        Workflow workflow = compensationFunc.getWorkflow();
        if (!(workflow.getUnitOfWork() instanceof SagaUnitOfWork uow)) {
            throw new IllegalArgumentException(
                    "Cannot retrive abort step from a unit of work of type %s expected type was %s"
                            .formatted(workflow.getUnitOfWork().getClass(), SagaUnitOfWork.class));
        }

        // TODO should the abort step depend on all steps or just the last one?
        Runnable abortAction = () -> uowService.abort(uow);
        var stepDeps = new ArrayList<FlowStep>(getSteps(compensationFunc));
        var abortStep = new Step(getAbortStepName(compensationFunc), abortAction, stepDeps);

        // workflow needs the abort step so it can execute it with executeUntilStep
        workflow.addStep(abortStep);
        return abortStep;
    }

    // TODO review if this function makes sense, or is hiding a desing issue
    public static List<String> getStepsWithCommitNames(WorkflowFunctionality func) {
        var stepNamesStream = getSteps(func).stream().map(FlowStep::getName);
        return Stream.concat(stepNamesStream, Stream.of(getCommitStepName(func))).toList();
    }

    // TODO review if this function makes sense, or is hiding a desing issue
    public static List<String> getCompensationStepsWithAbortNames(CompensationFunctionality compensationFunc) {
        var compensationStepNamesStream = getSteps(compensationFunc).stream().map(FlowStep::getName);
        return Stream.concat(compensationStepNamesStream, Stream.of(getAbortStepName(compensationFunc))).toList();
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

    public static String getCommitStepName(WorkflowFunctionality func) {
        // TODO should be changed for funcId or something uniquely identifiable
        return "%s-commit".formatted(func.getClass().getSimpleName());
    }

    public static String getAbortStepName(WorkflowFunctionality func) {
        // TODO should be changed for funcId or something uniquely identifiable
        return "%s-abort".formatted(func.getClass().getSimpleName());
    }
}
