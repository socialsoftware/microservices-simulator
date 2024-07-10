package pt.ulisboa.tecnico.socialsoftware.ms.coordination.workflow;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.ListIterator;
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
        HashMap<FlowStep, CompletableFuture<Void>> stepFutures = new HashMap<>();
        ListIterator<FlowStep> iterator = plan.listIterator();

        while (iterator.hasNext()) {
            FlowStep step = iterator.next();
            if (canExecute(stepFutures, step)) {
                stepFutures.put(step, step.execute(unitOfWork));
                iterator.remove();
                iterator = plan.listIterator();
            }
        }

        return CompletableFuture.allOf(stepFutures.values().toArray(new CompletableFuture[0]));
    }

    public CompletableFuture<Void> oldexecute(UnitOfWork unitOfWork) {
        HashMap<FlowStep, CompletableFuture<Void>> stepFutures = new HashMap<>();

        // Initialize futures for steps with no dependencies
        for (FlowStep step: plan) {
            if (dependencies.get(step).isEmpty()) {;
                stepFutures.put(step, step.execute(unitOfWork)); // executa e guarda os passos sem dependencias
            }
        }

        // Execute steps based on dependencies
        for (FlowStep step: plan) {
            if (!stepFutures.containsKey(step)) { // se for um step com dependencias

                ArrayList<FlowStep> deps = dependencies.get(step); // vai buscar todas as dependencias
                CompletableFuture<Void> combinedFuture = CompletableFuture.allOf( // cria um future que so executa quando todas as dependencias forem completadas
                    deps.stream().map(stepFutures::get).toArray(CompletableFuture[]::new) // mapeia cada dependencia com o correspondente future de step futures
                );
                stepFutures.put(step, combinedFuture.thenCompose(ignored -> step.execute(unitOfWork))); // so executa depois das dependencias estarem completadas
            }
        }

        // Wait for all steps to complete
        return CompletableFuture.allOf(stepFutures.values().toArray(new CompletableFuture[0]));
    }
}