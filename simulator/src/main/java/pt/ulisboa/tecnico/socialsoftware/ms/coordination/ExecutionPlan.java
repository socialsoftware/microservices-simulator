package pt.ulisboa.tecnico.socialsoftware.ms.coordination;

import pt.ulisboa.tecnico.socialsoftware.ms.transaction.unitOfWork.UnitOfWork;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ExecutionPlan {
    private ArrayList<FlowStep> plan;
    private HashMap<FlowStep, ArrayList<FlowStep>> dependencies;
    private HashMap<FlowStep, Boolean> executedSteps = new HashMap<>();
    private HashMap<FlowStep, CompletableFuture<Void>> stepFutures = new HashMap<>();
    private static final Logger logger = LoggerFactory.getLogger(ExecutionPlan.class);

    public ExecutionPlan(ArrayList<FlowStep> plan, HashMap<FlowStep, ArrayList<FlowStep>> dependencies,
            WorkflowFunctionality functionality) {
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

    public CompletableFuture<Void> execute(UnitOfWork unitOfWork) {
        return executeSteps(this.plan, unitOfWork);
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

    private CompletableFuture<Void> executeSteps(List<FlowStep> steps, UnitOfWork unitOfWork) {
        // Execute steps without dependencies first
        for (FlowStep step : steps) {
            if (dependencies.get(step).isEmpty()) {
                stepFutures.putIfAbsent(step, runStep(step, unitOfWork));
            }
        }

        // Execute steps with dependencies, ensuring all dependencies are executed first
        for (FlowStep step : steps) {
            if (!stepFutures.containsKey(step)) {
                CompletableFuture<Void> depsDone = CompletableFuture.allOf(
                        dependencies.get(step).stream()
                                .map(stepFutures::get)
                                .toArray(CompletableFuture[]::new));

                stepFutures.put(step, depsDone.thenCompose(ignored -> runStep(step, unitOfWork)));
            }
        }

        // Wait for all steps to complete and mark them as executed
        return CompletableFuture.allOf(
                steps.stream()
                        .map(stepFutures::get)
                        .toArray(CompletableFuture[]::new))
                .thenRun(() -> {
                    for (FlowStep step : steps) {
                        executedSteps.put(step, true);
                    }
                });
    }

    private CompletableFuture<Void> runStep(FlowStep step, UnitOfWork unitOfWork) {
        final String stepName = step.getName();
        final String funcName = unitOfWork.getFunctionalityName();

        logger.info("START EXECUTION STEP: {} with from functionality {}", stepName, funcName);
        CompletableFuture<Void> stepFuture = step.execute(unitOfWork);
        logger.info("END EXECUTION STEP: {} with from functionality {}", stepName, funcName);

        return stepFuture;
    }
}