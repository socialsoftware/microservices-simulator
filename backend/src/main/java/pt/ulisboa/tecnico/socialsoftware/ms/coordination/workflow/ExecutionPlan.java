package pt.ulisboa.tecnico.socialsoftware.ms.coordination.workflow;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.CompletableFuture;

import pt.ulisboa.tecnico.socialsoftware.ms.coordination.unitOfWork.UnitOfWork;

public class ExecutionPlan {
    private ArrayList<FlowStep> plan;
    private HashMap<FlowStep, ArrayList<FlowStep>> dependencies;
    private HashMap<FlowStep, Boolean> executedSteps = new HashMap<>();
    private HashMap<FlowStep, CompletableFuture<Void>> stepFutures = new HashMap<>();
    private WorkflowFunctionality functionality;

    public ExecutionPlan(ArrayList<FlowStep> plan, HashMap<FlowStep, ArrayList<FlowStep>> dependencies, WorkflowFunctionality functionality) {
        this.plan = plan;
        this.dependencies = dependencies;
        for (FlowStep step : plan) {
            executedSteps.put(step, false);
        }
        this.functionality = functionality;
    }

    public ArrayList<FlowStep> getPlan() {
        return this.plan;
    }

    public void setPlan(ArrayList<FlowStep> plan) {
        this.plan = plan;
    }

    /* 
     * while not plan.isempty
     *      do: step = getplan.first / getplan.next
     *          if canExecute(step): 
     *              step.exec()
     *              plan.remove(step)
     * 
     * canExecute(step):
     *      return stepFutures.containAll(step.dependencies)
    */

    public boolean canExecute(HashMap<FlowStep, CompletableFuture<Void>> stepFutures, FlowStep step) {
        return stepFutures.keySet().containsAll(step.getDependencies());
    }

    public CompletableFuture<Void> execute(UnitOfWork unitOfWork) {

        // Initialize futures for steps with no dependencies
        for (FlowStep step: plan) {
            if (dependencies.get(step).isEmpty()) {
                this.stepFutures.put(step, step.execute(unitOfWork)); // execute and save the steps with no dependencies
                executedSteps.put(step, true);
            }
        }

        // Execute steps based on dependencies
        for (FlowStep step: plan) {
            if (!this.stepFutures.containsKey(step)) { // if the step has dependencies
                ArrayList<FlowStep> deps = dependencies.get(step); // get all dependencies
                CompletableFuture<Void> combinedFuture = CompletableFuture.allOf( // create a future that only executes when all the dependencies are completed
                    deps.stream().map(this.stepFutures::get).toArray(CompletableFuture[]::new) // maps each dependency to its corresponding future in stepFutures
                );
                this.stepFutures.put(step, combinedFuture.thenCompose(ignored -> step.execute(unitOfWork))); // only executes after all dependencies are completed
            }
        }

        // Wait for all steps to complete
        return CompletableFuture.allOf(this.stepFutures.values().toArray(new CompletableFuture[0]));
    }


    public CompletableFuture<Void> executeSteps(List<FlowStep> steps, UnitOfWork unitOfWork) {

        for (FlowStep step : steps) {
            if (dependencies.get(step).isEmpty()) {
                this.stepFutures.put(step, step.execute(unitOfWork));
            }
        }

        for (FlowStep step : steps) {
            if (!stepFutures.containsKey(step)) {
                ArrayList<FlowStep> deps = dependencies.get(step);
                CompletableFuture<Void> combinedFuture = CompletableFuture.allOf(
                        deps.stream().map(this.stepFutures::get).toArray(CompletableFuture[]::new)
                );
                this.stepFutures.put(step, combinedFuture.thenCompose(ignored -> step.execute(unitOfWork)));
            }
        }

        return CompletableFuture.allOf(this.stepFutures.values().toArray(new CompletableFuture[0]))
                .thenRun(() -> {
                    for (FlowStep step : steps) {
                        executedSteps.put(step, true);
                    }
                });
    }

    public CompletableFuture<Void> executeUntilStep(FlowStep targetStep, UnitOfWork unitOfWork) {
        ArrayList<FlowStep> stepsToExecute = new ArrayList<>();
        for (FlowStep step : plan) {
            if (!executedSteps.get(step)) {
                stepsToExecute.add(step);
            }
    
            // Stop collecting steps once the target step is added to the list
            if (step.equals(targetStep)) {
                break;
            }
        }
        return executeSteps(stepsToExecute, unitOfWork);
    }

    public CompletableFuture<Void> resume(UnitOfWork unitOfWork) {
        ArrayList<FlowStep> remainingSteps = new ArrayList<>();
        for (FlowStep step : plan) {
            if (!executedSteps.get(step)) {
                remainingSteps.add(step);
            }
        }
        return executeSteps(remainingSteps, unitOfWork);
    }

    public FlowStep getStepByName(String stepName) {
        for (FlowStep step : plan) {
            if (step.getName().equals(stepName)) {
                return step;
            }
        }
        throw new IllegalArgumentException("Step with name: " + stepName + " not found.");
    }
}