package pt.ulisboa.tecnico.socialsoftware.ms.coordination.workflow;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.concurrent.CompletableFuture;

import pt.ulisboa.tecnico.socialsoftware.ms.coordination.unitOfWork.UnitOfWork;

public class ExecutionPlan {
    private ArrayList<FlowStep> plan;
    private HashMap<FlowStep, ArrayList<FlowStep>> dependencies;

    public ExecutionPlan(ArrayList<FlowStep> plan, HashMap<FlowStep, ArrayList<FlowStep>> dependencies) {
        this.plan = plan;
        this.dependencies = dependencies;
    }

    public ArrayList<FlowStep> getPlan() {
        return this.plan;
    }

    public void setPlan(ArrayList<FlowStep> plan) {
        this.plan = plan;
    }

    public CompletableFuture<Void> execute(UnitOfWork unitOfWork) {
        HashMap<FlowStep, CompletableFuture<Void>> stepFutures = new HashMap<>();

        // Initialize futures for steps with no dependencies
        for (FlowStep step: plan) {
            if (dependencies.get(step).isEmpty() /* && step instanceof AsyncStep */ ) {;
                stepFutures.put(step, step.execute(unitOfWork));
            }
        }

        // Execute steps based on dependencies
        for (FlowStep step: plan) {
            if (!stepFutures.containsKey(step)) {

                ArrayList<FlowStep> deps = dependencies.get(step);
                CompletableFuture<Void> combinedFuture = CompletableFuture.allOf(
                    deps.stream().map(stepFutures::get).toArray(CompletableFuture[]::new)
                );
                stepFutures.put(step, combinedFuture.thenCompose(ignored -> step.execute(unitOfWork)));
            }
        }

        // Wait for all steps to complete
        return CompletableFuture.allOf(stepFutures.values().toArray(new CompletableFuture[0]));
    }
}