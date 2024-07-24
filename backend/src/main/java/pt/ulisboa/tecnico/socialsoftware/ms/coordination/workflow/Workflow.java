package pt.ulisboa.tecnico.socialsoftware.ms.coordination.workflow;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.concurrent.CompletableFuture;

import pt.ulisboa.tecnico.socialsoftware.ms.coordination.unitOfWork.UnitOfWork;
import pt.ulisboa.tecnico.socialsoftware.ms.coordination.unitOfWork.UnitOfWorkService;

//TODO change how planOrder works
public abstract class Workflow {
    private UnitOfWorkService unitOfWorkService;
    private UnitOfWork unitOfWork;
    private WorkflowFunctionality data;
    protected HashMap<FlowStep, ArrayList<FlowStep>> stepsWithDependencies = new HashMap<>();
    private ExecutionPlan executionPlan; // redefined for each transaction model
    private HashMap<String, FlowStep> stepNameMap = new HashMap<>();
    private FlowStep lastExecutedStep;

    public Workflow(WorkflowFunctionality data, UnitOfWorkService unitOfWorkService, UnitOfWork unitOfWork) {
        this.data = data;
        this.unitOfWorkService = unitOfWorkService;
        this.unitOfWork = unitOfWork;
    }

    public Workflow(UnitOfWorkService unitOfWorkService, UnitOfWork unitOfWork) {
        this.unitOfWorkService = unitOfWorkService;
        this.unitOfWork = unitOfWork;
    }

    public UnitOfWork getUnitOfWork() {
        return unitOfWork;
    }

    public ExecutionPlan getExecutionPlan() {
        return this.executionPlan;
    }

    public void addStep(FlowStep step){
        this.stepsWithDependencies.put(step, step.getDependencies());
        stepNameMap.put(step.getName(), step);
    }

    public void executeStepByName(String stepName, UnitOfWork unitOfWork) {
        FlowStep step = getStepByName(stepName);
        executionPlan.executeSteps((ArrayList<FlowStep>) Collections.singletonList(step), unitOfWork).join();
        lastExecutedStep = step;
    }
    
    public void executeUntilStep(String stepName, UnitOfWork unitOfWork) {
        this.executionPlan = planOrder(this.stepsWithDependencies);
        FlowStep targetStep = getStepByName(stepName);
        executionPlan.executeUntilStep(targetStep, unitOfWork).join();
        lastExecutedStep = targetStep;
    }

    public CompletableFuture<Void> resume(UnitOfWork unitOfWork) {
        return executionPlan.resume(unitOfWork)
            .exceptionally(ex -> {
                unitOfWorkService.abort(unitOfWork);
                throw new RuntimeException(ex);
            }).thenRun(() -> unitOfWorkService.commit(unitOfWork));
    }

    private FlowStep getStepByName(String stepName) {
        FlowStep step = stepNameMap.get(stepName);
        if (step == null) {
            throw new IllegalArgumentException("Step with name " + stepName + " not found.");
        }
        return step;
    }

    public abstract ExecutionPlan planOrder(HashMap<FlowStep, ArrayList<FlowStep>> stepsWithDependencies);

    public CompletableFuture<Void> execute(UnitOfWork unitOfWork) {
        this.executionPlan = planOrder(this.stepsWithDependencies);
        return executionPlan.oldexecute(unitOfWork)
            .exceptionally(ex -> {
                unitOfWorkService.abort(unitOfWork);
                throw new RuntimeException(ex);
            }).thenRun(() -> unitOfWorkService.commit(unitOfWork));
    }
}
