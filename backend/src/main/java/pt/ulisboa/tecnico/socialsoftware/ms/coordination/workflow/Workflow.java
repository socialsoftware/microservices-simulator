package pt.ulisboa.tecnico.socialsoftware.ms.coordination.workflow;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;

import pt.ulisboa.tecnico.socialsoftware.ms.coordination.unitOfWork.UnitOfWork;
import pt.ulisboa.tecnico.socialsoftware.ms.coordination.unitOfWork.UnitOfWorkService;
import pt.ulisboa.tecnico.socialsoftware.ms.quizzes.microservices.exception.TutorException;

public abstract class Workflow {
    protected UnitOfWorkService unitOfWorkService;
    private UnitOfWork unitOfWork;
    private WorkflowFunctionality functionality;
    protected HashMap<FlowStep, ArrayList<FlowStep>> stepsWithDependencies = new HashMap<>();
    private ExecutionPlan executionPlan; // redefined for each transaction model
    private HashMap<String, FlowStep> stepNameMap = new HashMap<>();

    public Workflow(WorkflowFunctionality functionality, UnitOfWorkService unitOfWorkService, UnitOfWork unitOfWork) {
        this.functionality = functionality;
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

    public WorkflowFunctionality getFunctionality() {
        return this.functionality;
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
    }
    
    public void executeUntilStep(String stepName, UnitOfWork unitOfWork) {
        this.executionPlan = planOrder(this.stepsWithDependencies);
        FlowStep targetStep = getStepByName(stepName);
        executionPlan.executeUntilStep(targetStep, unitOfWork).join();
    }

    public CompletableFuture<Void> resume(UnitOfWork unitOfWork) {
        try {
            return executionPlan.resume(unitOfWork)
                .thenRun(() -> {
                    unitOfWorkService.commit(unitOfWork);
                })
                .exceptionally(ex -> {
                    Throwable cause = (ex instanceof CompletionException) ? ex.getCause() : ex;
    
                    unitOfWorkService.abort(unitOfWork);
    
                    if (cause instanceof TutorException) {
                        throw (TutorException) cause;
                    } else {
                        throw new RuntimeException(cause);  
                    }
                });
        } catch (TutorException e) {
            unitOfWorkService.abort(unitOfWork);
            throw e;
        }
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
        try {
            return executionPlan.execute(unitOfWork)
                .thenRun(() -> {
                    unitOfWorkService.commit(unitOfWork);
                })
                .exceptionally(ex -> {
                    Throwable cause = (ex instanceof CompletionException) ? ex.getCause() : ex;
    
                    unitOfWorkService.abort(unitOfWork);
    
                    if (cause instanceof TutorException) {
                        throw (TutorException) cause;
                    } else {
                        throw new RuntimeException(cause);  
                    }
                });
        } catch (TutorException e) {
            unitOfWorkService.abort(unitOfWork);
            throw e;
        }
    }
}
