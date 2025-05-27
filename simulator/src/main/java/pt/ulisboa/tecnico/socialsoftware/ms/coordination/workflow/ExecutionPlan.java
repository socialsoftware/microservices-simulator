package pt.ulisboa.tecnico.socialsoftware.ms.coordination.workflow;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Collectors;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.TimeUnit;

import pt.ulisboa.tecnico.socialsoftware.ms.coordination.unitOfWork.UnitOfWork;
import pt.ulisboa.tecnico.socialsoftware.ms.exception.SimulatorException;
import pt.ulisboa.tecnico.socialsoftware.ms.utils.BehaviourHandler;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class ExecutionPlan {
    private ArrayList<FlowStep> plan;
    private HashMap<FlowStep, ArrayList<FlowStep>> dependencies;
    private HashMap<FlowStep, Boolean> executedSteps = new HashMap<>();
    private HashMap<FlowStep, CompletableFuture<Void>> stepFutures = new HashMap<>();
    private WorkflowFunctionality functionality;
    private String functionalityName;
    private Map<String, List<Integer>> behaviour;
    private static final int DEFAULT_VALUE = 0;
    private static final int THROW_EXCEPTION = 1;

    private static final Logger logger = LoggerFactory.getLogger(ExecutionPlan.class);

    public ExecutionPlan(ArrayList<FlowStep> plan, HashMap<FlowStep, ArrayList<FlowStep>> dependencies, WorkflowFunctionality functionality) {
        this.plan = plan;
        this.dependencies = dependencies;
        for (FlowStep step : plan) {
            executedSteps.put(step, false);
        }
        this.functionality = functionality;
        this.functionalityName = functionality.getClass().getSimpleName();

        behaviour = BehaviourHandler.getInstance().loadStepsFile(functionalityName);
        BehaviourHandler.getInstance().appendToReport(reportSteps(behaviour));

    }

    public ArrayList<FlowStep> getPlan() {
        return this.plan;
    }

    public void setPlan(ArrayList<FlowStep> plan) {
        this.plan = plan;
    }

    public int getTotalDelay() {
        int totalDelay = 0;
        int delayBeforeValue = 1;
        int delayAfterValue = 2;
        for (FlowStep step : plan) {
            if (behaviour.containsKey(step.getName())) {
                totalDelay += behaviour.get(step.getName()).get(delayBeforeValue) + behaviour.get(step.getName()).get(delayAfterValue);
            }
        }
        return totalDelay;
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
        List<Integer> behaviourValues = new ArrayList<>();
        
        // Initialize futures for steps with no dependencies
        for (FlowStep step: plan) {
            final String stepName = step.getName();
            final String funcName = functionalityName;

            // Check if the step is in the behaviour map
            final int faultValue = behaviour.containsKey(stepName) ? behaviour.get(stepName).get(0) : DEFAULT_VALUE;
            final int delayBeforeValue = behaviour.containsKey(stepName) ? behaviour.get(stepName).get(1) : DEFAULT_VALUE;
            final int delayAfterValue = behaviour.containsKey(stepName) ? behaviour.get(stepName).get(2) : DEFAULT_VALUE;
            if (faultValue == THROW_EXCEPTION) { 
                throw new SimulatorException(stepName + " Microservice not available");

            }
            if (dependencies.get(step).isEmpty()) {
                this.stepFutures.put(step, CompletableFuture.completedFuture(null)
                .thenAccept(ignored -> {
                    try {
                        Thread.sleep(delayBeforeValue);
                        logger.info("START EXECUTION STEP: {} with from functionality {}", stepName, funcName);
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        throw new CompletionException(e);
                    }
                })
                .thenCompose(ignored -> step.execute(unitOfWork))
                .thenAccept(ignored -> {
                    try {
                        logger.info("END EXECUTION STEP: {} with from functionality {}", stepName, funcName);
                        Thread.sleep(delayAfterValue);
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        throw new CompletionException(e);
                    }
                })
            ); // Execute and save the steps with no dependencies
                executedSteps.put(step, true);
            }
            
        }

        // Execute steps based on dependencies
        for (FlowStep step: plan) {
            final String stepName = step.getName();
            final String funcName = functionalityName;
            final int faultValue = behaviour.containsKey(stepName) ? behaviour.get(stepName).get(0) : DEFAULT_VALUE;
            final int delayBeforeValue = behaviour.containsKey(stepName) ? behaviour.get(stepName).get(1) : DEFAULT_VALUE;
            final int delayAfterValue = behaviour.containsKey(stepName) ? behaviour.get(stepName).get(2) : DEFAULT_VALUE;

            if (faultValue == THROW_EXCEPTION) {   
                throw new SimulatorException(stepName + " Microservice not available");
            }
            if (!this.stepFutures.containsKey(step) ) { // if the step has dependencies         
                ArrayList<FlowStep> deps = dependencies.get(step); // get all dependencies
                CompletableFuture<Void> combinedFuture = CompletableFuture.allOf( // create a future that only executes when all the dependencies are completed
                    deps.stream().map(this.stepFutures::get).toArray(CompletableFuture[]::new) // maps each dependency to its corresponding future in stepFutures
                );
                this.stepFutures.put(step,combinedFuture
                    .thenAccept(ignored -> {
                        try {
                            Thread.sleep(delayBeforeValue);
                            logger.info("START EXECUTION STEP: {} with from functionality {}", stepName, funcName);
                        } catch (InterruptedException e) {
                            Thread.currentThread().interrupt();
                            throw new CompletionException(e);
                        }
                    })
                    .thenCompose(ignored -> step.execute(unitOfWork))
                    .thenAccept(ignored -> {
                        try {
                            logger.info("END EXECUTION STEP: {} with from functionality {}", stepName, funcName);
                            Thread.sleep(delayAfterValue);
                        } catch (InterruptedException e) {
                            Thread.currentThread().interrupt();
                            throw new CompletionException(e);
                        }
                    })
                );
                executedSteps.put(step, true);
            }
            
        }

        // Wait for all steps to complete
        return CompletableFuture.allOf(this.stepFutures.values().toArray(new CompletableFuture[0]));
    }

    private String reportSteps(Map<String, List<Integer>> behaviour) {
        List<String> nonCommonSteps = new ArrayList<>();
        List<String> misMatchSteps = new ArrayList<>();
        List <String> planSteps = plan.stream().map(FlowStep::getName).collect(Collectors.toList());
        for (FlowStep step : plan) {
            String stepName = step.getName();
            if (!behaviour.containsKey(stepName))
                nonCommonSteps.add(stepName);
        }
        for (String step: behaviour.keySet()) {
            if (!planSteps.contains(step)) {
                misMatchSteps.add(step);
            }
        }
        
        StringBuilder report = new StringBuilder(); // For file (plain)
        StringBuilder colorReport = new StringBuilder(); // For terminal (with color)

        if (!behaviour.isEmpty()) {
            report.append("Functionality: ").append(functionalityName).append("\n");
            colorReport.append("Functionality: ").append(functionalityName).append("\n");

            report.append("Behaviour: ").append(behaviour).append("\n");
            colorReport.append("Behaviour: ").append(behaviour).append("\n");

            report.append("Steps: ").append(planSteps).append("\n");
            colorReport.append("Steps: ").append(planSteps).append("\n");

            boolean mismatchExists = !misMatchSteps.isEmpty();
            boolean nonCommonDiffers = !nonCommonSteps.isEmpty();

            // Non Defined Steps
            report.append("Non Defined Steps: ").append(nonCommonSteps).append("\n");
            if (nonCommonDiffers) {
                colorReport.append("\u001B[33m").append("Non Defined Steps: ").append(nonCommonSteps).append("\u001B[0m").append("\n");
            } else {
                colorReport.append("Non Defined Steps: ").append(nonCommonSteps).append("\n");
            }

            // Mismatch Steps
            report.append("Mismatch Steps: ").append(misMatchSteps).append("\n");
            if (mismatchExists) {
                colorReport.append("\u001B[31m").append("Mismatch Steps: ").append(misMatchSteps).append("\u001B[0m").append("\n");
            } else {
                colorReport.append("Mismatch Steps: ").append(misMatchSteps).append("\n");
            }

            // Log colored version
            logger.info(colorReport.toString());

            // Optional terminal summary
            if (mismatchExists) {
                logger.error("\u001B[31mMismatch detected\u001B[0m");
            } if (nonCommonDiffers) {
                logger.warn("\u001B[33mCommon steps differ from expected plan\u001B[0m");
            } else {
                logger.info("\u001B[32mBehaviour matches expectations\u001B[0m");
            }

        }

        return report.toString();
    }
    
    


    public CompletableFuture<Void> executeSteps(List<FlowStep> steps, UnitOfWork unitOfWork) {

        String stepName;
        List<Integer> behaviourValues = new ArrayList<>();


        for (FlowStep step: steps) {
            stepName = step.getName();

            // Check if the step is in the behaviour map
            behaviourValues = behaviour.containsKey(stepName) ? behaviour.get(stepName) : Arrays.asList(DEFAULT_VALUE,DEFAULT_VALUE,DEFAULT_VALUE);
            if (behaviourValues.get(0) == THROW_EXCEPTION) {   
                throw new SimulatorException(stepName + " Microservice not available");

            }
            if (dependencies.get(step).isEmpty()) {
                this.stepFutures.put(step, step.execute(unitOfWork)); // Execute and save the steps with no dependencies
            }
        }
            

        for (FlowStep step: steps) {
            stepName = step.getName();
            behaviourValues = behaviour.containsKey(stepName) ? behaviour.get(stepName) : Arrays.asList(DEFAULT_VALUE,DEFAULT_VALUE,DEFAULT_VALUE);
            if (behaviourValues.get(0) == THROW_EXCEPTION) {   
                throw new SimulatorException(stepName + " Microservice not available");
            }
            if (!this.stepFutures.containsKey(step) ) { // if the step has dependencies         
                ArrayList<FlowStep> deps = dependencies.get(step); // get all dependencies
                CompletableFuture<Void> combinedFuture = CompletableFuture.allOf( // create a future that only executes when all the dependencies are completed
                    deps.stream().map(this.stepFutures::get).toArray(CompletableFuture[]::new) // maps each dependency to its corresponding future in stepFutures
                );
                this.stepFutures.put(step, combinedFuture.thenCompose(ignored -> step.execute(unitOfWork))); // only executes after all dependencies are completed
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