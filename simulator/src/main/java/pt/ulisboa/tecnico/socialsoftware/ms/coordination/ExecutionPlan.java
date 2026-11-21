package pt.ulisboa.tecnico.socialsoftware.ms.coordination;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pt.ulisboa.tecnico.socialsoftware.ms.transaction.unitOfWork.UnitOfWork;

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

    public boolean canExecute(HashMap<FlowStep, CompletableFuture<Void>> stepFutures, FlowStep step) {
        return stepFutures.keySet().containsAll(step.getDependencies());
    }

    public CompletableFuture<Void> execute(UnitOfWork unitOfWork) {
        // Worklist scheduling: repeatedly scan the plan for steps whose dependencies
        // already
        // have a registered future (canExecute()) and schedule them, instead of relying
        // on a
        // fixed two-pass (root / non-root) structure that implicitly assumes `plan` is
        // already
        // topologically sorted. This tolerates arbitrary dependency depth and arbitrary
        // registration order. The fault-flag check happens inside the .thenAccept
        // lambda, so it
        // only fires once the step's real dependency chain (or, for root steps,
        // immediately) has
        // actually completed - never synchronously ahead of that.
        ArrayList<FlowStep> remaining = new ArrayList<>(plan);
        return executeSteps(remaining, unitOfWork);
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

    public CompletableFuture<Void> executeSteps(List<FlowStep> steps, UnitOfWork unitOfWork) {
        while (!steps.isEmpty()) {
            boolean scheduledAny = false;
            Iterator<FlowStep> it = steps.iterator();
            while (it.hasNext()) {
                FlowStep step = it.next();
                if (!canExecute(this.stepFutures, step)) {
                    continue; // dependencies not yet scheduled; try again in a later scan
                }

                ArrayList<FlowStep> deps = dependencies.get(step);
                CompletableFuture<Void> readyFuture = deps.isEmpty()
                        ? CompletableFuture.completedFuture(null)
                        : CompletableFuture.allOf(
                                deps.stream().map(this.stepFutures::get).toArray(CompletableFuture[]::new));

                this.stepFutures.put(step, readyFuture
                        .thenCompose(ignored -> runStep(step, unitOfWork)));

                executedSteps.put(step, true);
                it.remove();
                scheduledAny = true;
            }

            if (!scheduledAny) {
                // No progress made in a full pass over the remaining steps: the dependency
                // graph
                // cannot be fully scheduled (e.g. a cycle, or a dependency on a step outside
                // `plan`).
                throw new IllegalStateException("Unable to schedule remaining steps in ExecutionPlan: "
                        + steps.stream().map(FlowStep::getName).collect(Collectors.joining(", ")));
            }
        }

        // Wait for all steps to complete
        return CompletableFuture.allOf(this.stepFutures.values().toArray(new CompletableFuture[0]));
    }

    private CompletableFuture<Void> runStep(FlowStep step, UnitOfWork unitOfWork) {
        final String stepName = step.getName();
        final String funcName = unitOfWork.getFunctionalityName();

        logger.info("START EXECUTION STEP: {} with from functionality {}", stepName, funcName);
        CompletableFuture<Void> stepFuture;
        try {
            stepFuture = step.execute(unitOfWork);
        } catch (RuntimeException e) {
            stepFuture = CompletableFuture.failedFuture(e);
        }
        logger.info("END EXECUTION STEP: {} with from functionality {}", stepName, funcName);

        return stepFuture;
    }
}