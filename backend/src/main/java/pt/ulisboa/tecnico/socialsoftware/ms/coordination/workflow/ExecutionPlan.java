package pt.ulisboa.tecnico.socialsoftware.ms.coordination.workflow;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.ListIterator;
import java.util.concurrent.CompletableFuture;

import pt.ulisboa.tecnico.socialsoftware.ms.coordination.unitOfWork.UnitOfWork;

public class ExecutionPlan {
    private ArrayList<FlowStep> plan;
    private HashMap<FlowStep, ArrayList<FlowStep>> dependencies;
    private HashMap<FlowStep, Boolean> executedSteps = new HashMap<>();
    private HashMap<FlowStep, CompletableFuture<Void>> stepFutures = new HashMap<>();

    public ExecutionPlan(ArrayList<FlowStep> plan, HashMap<FlowStep, ArrayList<FlowStep>> dependencies) {
        this.plan = plan;
        this.dependencies = dependencies;
        for (FlowStep step : plan) {
            executedSteps.put(step, false);
        }
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
     *          if canExec(step): 
     *              step.exec()
     *              plan.remove(step)
     * 
     * canExec(step):
     *      return stepFutures.containAll(step.dependencies)
    */

    public boolean canExecute(HashMap<FlowStep, CompletableFuture<Void>> stepFutures, FlowStep step) {
        return stepFutures.keySet().containsAll(step.getDependencies());
    }

    public CompletableFuture<Void> execute(UnitOfWork unitOfWork) {
        ListIterator<FlowStep> iterator = plan.listIterator();

        while (iterator.hasNext()) {
            FlowStep step = iterator.next();
            if (canExecute(this.stepFutures, step)) {
                this.stepFutures.put(step, step.execute(unitOfWork));
                iterator.remove();
                iterator = plan.listIterator();
            }
        }

        return CompletableFuture.allOf(this.stepFutures.values().toArray(new CompletableFuture[0]));
    }

    public CompletableFuture<Void> oldexecute(UnitOfWork unitOfWork) {

        // Initialize futures for steps with no dependencies
        for (FlowStep step: plan) {
            if (dependencies.get(step).isEmpty()) {;
                this.stepFutures.put(step, step.execute(unitOfWork)); // executa e guarda os passos sem dependencias
            }
        }

        // Execute steps based on dependencies
        for (FlowStep step: plan) {
            if (!this.stepFutures.containsKey(step)) { // se for um step com dependencias
                ArrayList<FlowStep> deps = dependencies.get(step); // vai buscar todas as dependencias
                CompletableFuture<Void> combinedFuture = CompletableFuture.allOf( // cria um future que so executa quando todas as dependencias forem completadas
                    deps.stream().map(this.stepFutures::get).toArray(CompletableFuture[]::new) // mapeia cada dependencia com o correspondente future de step futures
                );
                this.stepFutures.put(step, combinedFuture.thenCompose(ignored -> step.execute(unitOfWork))); // so executa depois das dependencias estarem completadas
            }
        }

        // Wait for all steps to complete
        return CompletableFuture.allOf(this.stepFutures.values().toArray(new CompletableFuture[0]));
    }
    
    public CompletableFuture<Void> executeNextStep(UnitOfWork unitOfWork) {
        for (FlowStep step : plan) {
            if (!executedSteps.get(step) && dependencies.get(step).stream().allMatch(dep -> executedSteps.get(dep))) {
                executedSteps.put(step, true);
                return step.execute(unitOfWork).thenRun(() -> { /* Next step will be executed in test case */ });
            }
        }
        return CompletableFuture.completedFuture(null);
    }

    public CompletableFuture<Void> executeSteps(ArrayList<FlowStep> steps, UnitOfWork unitOfWork) {

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
            stepsToExecute.add(step);
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
        throw new IllegalArgumentException("Step with name " + stepName + " not found.");
    }
}