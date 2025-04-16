package pt.ulisboa.tecnico.socialsoftware.ms.coordination.workflow;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.*;
import java.util.stream.Collectors;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import pt.ulisboa.tecnico.socialsoftware.ms.coordination.unitOfWork.UnitOfWork;
import pt.ulisboa.tecnico.socialsoftware.ms.utils.ReadStepsFile;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class ExecutionPlan {
    private ArrayList<FlowStep> plan;
    private HashMap<FlowStep, ArrayList<FlowStep>> dependencies;
    private HashMap<FlowStep, Boolean> executedSteps = new HashMap<>();
    private HashMap<FlowStep, CompletableFuture<Void>> stepFutures = new HashMap<>();
    private WorkflowFunctionality functionality;
    private String functionalityName;

    private static final Logger logger = LoggerFactory.getLogger(ExecutionPlan.class);

    public ExecutionPlan(ArrayList<FlowStep> plan, HashMap<FlowStep, ArrayList<FlowStep>> dependencies, WorkflowFunctionality functionality) {
        this.plan = plan;
        this.dependencies = dependencies;
        for (FlowStep step : plan) {
            executedSteps.put(step, false);
        }
        this.functionality = functionality;
        this.functionalityName = functionality.getClass().getSimpleName();

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

        Map<String, List<Integer>> behaviour = ReadStepsFile.getInstance().loadStepsFile(functionalityName);
        if (!behaviour.isEmpty()) {
            behaviour.forEach((key, value) -> System.out.println(key + " -> " + value));
        }
        ReadStepsFile.getInstance().appendToReport(reportSteps(behaviour));
        String stepName;

        // Initialize futures for steps with no dependencies
        for (FlowStep step: plan) {
            stepName = step.getName();

            // Check if the step is in the behaviour map
            final int faultValue = behaviour.containsKey(stepName) ? behaviour.get(stepName).get(0) : 1;
            final int delayBeforeValue = behaviour.containsKey(stepName) ? behaviour.get(stepName).get(1) : 0;
            final int delayAfterValue = behaviour.containsKey(stepName) ? behaviour.get(stepName).get(2) : 0;
            if(faultValue == 1) {   
                if (dependencies.get(step).isEmpty()) {
                    this.stepFutures.put(step, step.execute(unitOfWork)); // Execute and save the steps with no dependencies
                    executedSteps.put(step, true);
                }
            }
        }

        // Execute steps based on dependencies
        for (FlowStep step: plan) {
            stepName = step.getName();
            final int faultValue = behaviour.containsKey(stepName) ? behaviour.get(stepName).get(0) : 1;
            final int delayBeforeValue = behaviour.containsKey(stepName) ? behaviour.get(stepName).get(1) : 0;
            final int delayAfterValue = behaviour.containsKey(stepName) ? behaviour.get(stepName).get(2) : 0;
            if (faultValue == 1) {
                if (!this.stepFutures.containsKey(step) ) { // if the step has dependencies      
                    
                    ArrayList<FlowStep> deps = dependencies.get(step); // get all dependencies
                    CompletableFuture<Void> combinedFuture = CompletableFuture.allOf( // create a future that only executes when all the dependencies are completed
                        deps.stream().map(this.stepFutures::get).toArray(CompletableFuture[]::new) // maps each dependency to its corresponding future in stepFutures
                    );
                    this.stepFutures.put(step, combinedFuture.thenCompose(ignored -> step.execute(unitOfWork))); // only executes after all dependencies are completed
                    executedSteps.put(step, true);
                }
            }
        }

        // Wait for all steps to complete
        return CompletableFuture.allOf(this.stepFutures.values().toArray(new CompletableFuture[0]));
    }

    private String reportSteps(Map<String, List<Integer>> behaviour) {
        List<String> commonSteps = new ArrayList<>();
        List<String> misMatchSteps = new ArrayList<>();
        List <String> planSteps = plan.stream().map(FlowStep::getName).collect(Collectors.toList());
        for (FlowStep step : plan) {
            String stepName = step.getName();
            if (behaviour.containsKey(stepName))
                commonSteps.add(stepName);
        }
        for (String step: behaviour.keySet()) {
            if (!commonSteps.contains(step)) {
                misMatchSteps.add(step);
            }
        }
        
        StringBuilder report = new StringBuilder();
        if(!behaviour.isEmpty()) {
            report.append("Functionality: ").append(functionalityName).append("\n");
            report.append("Behaviour: ").append(behaviour).append("\n");
            report.append("Steps: ").append(planSteps).append("\n");
            report.append("Common Steps: ").append(commonSteps).append("\n");
            report.append("Mismatch Steps: ").append(misMatchSteps).append("\n");
            
            String reportString = report.toString();

            logger.info( "\u001B[34m" + reportString + "\u001B[0m");
            if (!misMatchSteps.isEmpty()) {
                logger.error("\u001B[31m" + "Mismatch detected\n" + "\u001B[0m");
            } if (!new HashSet<>(commonSteps).equals(new HashSet<>(planSteps))) {
                logger.warn("\u001B[33m" + "Common steps differ from expected plan\n" + "\u001B[0m");
            } if (misMatchSteps.isEmpty() && new HashSet<>(commonSteps).equals(new HashSet<>(planSteps))) {
                logger.info("\u001B[32m" + "Behaviour matches expectations\n" + "\u001B[0m");
            }

        }
        return report.toString();
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


