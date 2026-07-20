package pt.ulisboa.tecnico.socialsoftware.ms.coordination;

import io.opentelemetry.api.trace.Span;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pt.ulisboa.tecnico.socialsoftware.ms.exception.SimulatorException;
import pt.ulisboa.tecnico.socialsoftware.ms.faults.FaultVectorInjectedFaultException;
import pt.ulisboa.tecnico.socialsoftware.ms.faults.FaultVectorProviderHolder;
import pt.ulisboa.tecnico.socialsoftware.ms.impairment.ImpairmentHandler;
import pt.ulisboa.tecnico.socialsoftware.ms.monitoring.TraceManager;
import pt.ulisboa.tecnico.socialsoftware.ms.monitoring.dynamic.DynamicEvidenceContext;
import pt.ulisboa.tecnico.socialsoftware.ms.monitoring.dynamic.DynamicEvidenceRecorderHolder;
import pt.ulisboa.tecnico.socialsoftware.ms.transaction.unitOfWork.UnitOfWork;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.stream.Collectors;


public class ExecutionPlan {
    private ArrayList<FlowStep> plan;
    private HashMap<FlowStep, ArrayList<FlowStep>> dependencies;
    private HashMap<FlowStep, Boolean> executedSteps = new HashMap<>();
    private HashMap<FlowStep, CompletableFuture<Void>> stepFutures = new HashMap<>();
    private WorkflowFunctionality functionality;
    private String functionalityName;
    private String functionalityClassFqn;
    private String functionalityClassSimpleName;
    private Map<String, List<Integer>> behaviour;
    private static final int DEFAULT_VALUE = 0;
    private static final int THROW_EXCEPTION = 1;
    private TraceManager traceManager;

    private static final Logger logger = LoggerFactory.getLogger(ExecutionPlan.class);

    public ExecutionPlan(ArrayList<FlowStep> plan, HashMap<FlowStep, ArrayList<FlowStep>> dependencies, WorkflowFunctionality functionality) {
        this.plan = plan;
        this.dependencies = dependencies;
        for (FlowStep step : plan) {
            executedSteps.put(step, false);
        }
        this.functionality = functionality;
        this.functionalityClassFqn = functionality.getClass().getName();
        this.functionalityClassSimpleName = functionality.getClass().getSimpleName();
        this.functionalityName = functionalityClassSimpleName;

        if (FaultVectorProviderHolder.isActive()) {
            behaviour = new LinkedHashMap<>();
        } else {
            behaviour = ImpairmentHandler.getInstance().loadStepsFile(functionalityName);
            ImpairmentHandler.getInstance().appendToReport(reportSteps(behaviour));
        }
        
        this.traceManager = TraceManager.getInstance();
    }

    public ArrayList<FlowStep> getPlan() {
        return this.plan;
    }

    public void setPlan(ArrayList<FlowStep> plan) {
        this.plan = plan;
    }

    public int getTotalDelay() {
        if (FaultVectorProviderHolder.isActive()) {
            return 0;
        }
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

    public String getBehaviour() {
        return behaviour.toString();
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
            final String stepName = step.getName();
            final String funcName = (unitOfWork != null)
                ? unitOfWork.getFunctionalityName()
                : this.functionalityName;

            // Check if the step is in the behaviour map
            List<Integer> behaviourValues = behaviourValues(stepName);
            final int faultValue = behaviourValues.get(0);
            final int delayBeforeValue = behaviourValues.get(1);
            final int delayAfterValue = behaviourValues.get(2);
            if (faultValue == THROW_EXCEPTION) {
                logger.info("EXCEPTION THROWN: {} with version {}", funcName, unitOfWork.getVersion());

                throw new SimulatorException("Fault on " + stepName );

            }
            if (dependencies.get(step).isEmpty()) {
                this.stepFutures.put(step, CompletableFuture.completedFuture(null)
                    .thenCompose(ignored -> executeInstrumentedStep(step, unitOfWork, funcName, stepName,
                            delayBeforeValue, delayAfterValue))
                ); // Execute and save the steps with no dependencies
                executedSteps.put(step, true);
            }
            
        }

        // Execute steps based on dependencies
        for (FlowStep step: plan) {
            final String stepName = step.getName();
            final String funcName = (unitOfWork != null)
                ? unitOfWork.getFunctionalityName()
                : this.functionalityName;
            List<Integer> behaviourValues = behaviourValues(stepName);
            final int faultValue = behaviourValues.get(0);
            final int delayBeforeValue = behaviourValues.get(1);
            final int delayAfterValue = behaviourValues.get(2);

            if (faultValue == THROW_EXCEPTION) {  
                logger.info("EXCEPTION THROWN: {} with version {}", funcName, unitOfWork.getVersion()); 
                throw new SimulatorException("Fault on " + stepName );
            }
            if (!this.stepFutures.containsKey(step) ) { // if the step has dependencies         
                ArrayList<FlowStep> deps = dependencies.get(step); // get all dependencies
                CompletableFuture<Void> combinedFuture = CompletableFuture.allOf( // create a future that only executes when all the dependencies are completed
                    deps.stream().map(this.stepFutures::get).toArray(CompletableFuture[]::new) // maps each dependency to its corresponding future in stepFutures
                );
                this.stepFutures.put(step,combinedFuture
                    .thenCompose(ignored -> executeInstrumentedStep(step, unitOfWork, funcName, stepName,
                            delayBeforeValue, delayAfterValue))
                );
                executedSteps.put(step, true);
            }
            
        }

        // Wait for all steps to complete
        return CompletableFuture.allOf(this.stepFutures.values().toArray(new CompletableFuture[0]));
    }

    private CompletableFuture<Void> executeInstrumentedStep(FlowStep step, UnitOfWork unitOfWork, String funcName,
                                                            String stepName, int delayBeforeValue, int delayAfterValue) {
        injectFaultIfAssigned(stepName);

        Long unitOfWorkVersion = unitOfWork != null ? unitOfWork.getVersion() : null;
        DynamicEvidenceContext.Scope scope = DynamicEvidenceContext.enterStep(funcName, functionalityClassFqn,
                functionalityClassSimpleName, stepName, unitOfWorkVersion);
        DynamicEvidenceContext.StepContext context = scope.context();
        DynamicEvidenceRecorderHolder.recordStepStarted(context);

        try {
            startStepTrace(funcName, stepName);
            runDelay(funcName, stepName, delayBeforeValue, true);
            logger.info("START EXECUTION STEP: {} with from functionality {}", stepName, funcName);

            CompletableFuture<Void> stepFuture = step.execute(unitOfWork);
            if (stepFuture == null) {
                stepFuture = CompletableFuture.completedFuture(null);
            }

            if (stepFuture.isDone()) {
                return stepFuture.whenComplete((ignored, error) -> finishInstrumentedStep(context, scope, funcName,
                        stepName, delayAfterValue, error));
            }

            // Dynamic evidence step context uses ThreadLocal state. For async continuations that complete on
            // a different thread, we close the originating scope here and rely on the captured StepContext when
            // emitting STEP_FINISHED. This first implementation slice therefore supports local/synchronous smoke
            // scenarios for in-step context lookup, but does not propagate ThreadLocal context across thread hops.
            scope.close();
            return stepFuture.whenComplete((ignored, error) -> finishInstrumentedStep(context, null, funcName,
                    stepName, delayAfterValue, error));
        } catch (Throwable error) {
            finishInstrumentedStep(context, scope, funcName, stepName, delayAfterValue, error);
            throw error;
        }
    }

    private void finishInstrumentedStep(DynamicEvidenceContext.StepContext context, DynamicEvidenceContext.Scope scope,
                                        String funcName, String stepName, int delayAfterValue, Throwable error) {
        try {
            if (error == null) {
                logger.info("END EXECUTION STEP: {} with from functionality {}", stepName, funcName);
                try {
                    runDelay(funcName, stepName, delayAfterValue, false);
                } catch (CompletionException delayError) {
                    DynamicEvidenceRecorderHolder.recordStepFinished(context, "ERROR", delayError);
                    throw delayError;
                }
                DynamicEvidenceRecorderHolder.recordStepFinished(context, "SUCCESS", null);
            } else {
                DynamicEvidenceRecorderHolder.recordStepFinished(context, "ERROR", error);
            }
        } finally {
            endStepTrace(funcName, stepName);
            if (scope != null) {
                scope.close();
            }
        }
    }

    private void startStepTrace(String funcName, String stepName) {
        TraceManager traceManager = TraceManager.getInstance();
        if (traceManager != null) {
            traceManager.startStepSpan(funcName, stepName);
        }
    }

    private void endStepTrace(String funcName, String stepName) {
        TraceManager traceManager = TraceManager.getInstance();
        if (traceManager != null) {
            traceManager.endStepSpan(funcName, stepName);
        }
    }

    private void runDelay(String funcName, String stepName, int delay, boolean isBefore) {
        try {
            TraceManager traceManager = TraceManager.getInstance();
            Span delaySpan = traceManager != null ? traceManager.startDelaySpan(funcName, stepName, delay, isBefore) : null;
            Thread.sleep(delay);
            if (traceManager != null) {
                traceManager.endDelaySpan(delaySpan);
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new CompletionException(e);
        }
    }

    private List<Integer> behaviourValues(String stepName) {
        if (FaultVectorProviderHolder.isActive()) {
            return Arrays.asList(DEFAULT_VALUE, DEFAULT_VALUE, DEFAULT_VALUE);
        }
        return behaviour.containsKey(stepName)
                ? behaviour.get(stepName)
                : Arrays.asList(DEFAULT_VALUE, DEFAULT_VALUE, DEFAULT_VALUE);
    }

    private void injectFaultIfAssigned(String stepName) {
        FaultVectorProviderHolder.currentBoundary()
                .filter(context -> stepName.equals(context.runtimeStepName()))
                .flatMap(ignored -> FaultVectorProviderHolder.faultForCurrentBoundary())
                .ifPresent(fault -> {
                    throw new FaultVectorInjectedFaultException(fault);
                });
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
                colorReport.append("\u001B[38;5;208m").append("Mismatch Steps: ").append(misMatchSteps).append("\u001B[0m").append("\n");
            } else {
                colorReport.append("Mismatch Steps: ").append(misMatchSteps).append("\n");
            }

            // Log colored version
            logger.info(colorReport.toString());

            // Optional terminal summary
            if (mismatchExists) {
                logger.error("\u001B[38;5;208mMismatch detected\u001B[0m");
            } if (nonCommonDiffers) {
                logger.warn("\u001B[33mCommon steps differ from expected plan\u001B[0m");
            } else {
                logger.info("\u001B[32mBehaviour matches expectations\u001B[0m");
            }

        }

        return report.toString();
    }
    
    


    public CompletableFuture<Void> executeSteps(List<FlowStep> steps, UnitOfWork unitOfWork) {

        final String funcName = (unitOfWork != null)
                ? unitOfWork.getFunctionalityName()
                : this.functionalityName;

        for (FlowStep step: steps) {
            final String stepName = step.getName();

            // Check if the step is in the behaviour map
            List<Integer> behaviourValues = behaviourValues(stepName);
            if (behaviourValues.get(0) == THROW_EXCEPTION) {
                logger.info("EXCEPTION THROWN: {} with version {}", funcName, unitOfWork != null ? unitOfWork.getVersion() : null);
                throw new SimulatorException("Fault on " + stepName );

            }
            if (dependencies.get(step).isEmpty() && !this.stepFutures.containsKey(step)) {
                final int delayBeforeValue = behaviourValues.get(1);
                final int delayAfterValue = behaviourValues.get(2);
                this.stepFutures.put(step, CompletableFuture.completedFuture(null)
                        .thenCompose(ignored -> executeInstrumentedStep(step, unitOfWork, funcName, stepName,
                                delayBeforeValue, delayAfterValue))
                ); // Execute and save the steps with no dependencies
            }
        }

        for (FlowStep step: steps) {
            final String stepName = step.getName();
            List<Integer> behaviourValues = behaviourValues(stepName);
            if (behaviourValues.get(0) == THROW_EXCEPTION) {
                logger.info("EXCEPTION THROWN: {} with version {}", funcName, unitOfWork != null ? unitOfWork.getVersion() : null);
                throw new SimulatorException("Fault on " + stepName );
            }
            if (!this.stepFutures.containsKey(step) ) { // if the step has dependencies
                ArrayList<FlowStep> deps = dependencies.get(step); // get all dependencies
                CompletableFuture<Void> combinedFuture = CompletableFuture.allOf( // create a future that only executes when all the dependencies are completed
                    deps.stream().map(this.stepFutures::get).toArray(CompletableFuture[]::new) // maps each dependency to its corresponding future in stepFutures
                );
                final int delayBeforeValue = behaviourValues.get(1);
                final int delayAfterValue = behaviourValues.get(2);
                this.stepFutures.put(step, combinedFuture
                        .thenCompose(ignored -> executeInstrumentedStep(step, unitOfWork, funcName, stepName,
                                delayBeforeValue, delayAfterValue))
                ); // only executes after all dependencies are completed
            }
            
        }

        return CompletableFuture.allOf(this.stepFutures.values().toArray(new CompletableFuture[0]))
                .thenRun(() -> {
                    for (FlowStep step : steps) {
                        executedSteps.put(step, true);
                    }
                });
    }

    public CompletableFuture<Void> executeStepForExecutor(FlowStep targetStep, UnitOfWork unitOfWork) {
        if (!executedSteps.containsKey(targetStep)) {
            throw new IllegalArgumentException("Step with name " + targetStep.getName() + " is not part of this execution plan.");
        }
        if (Boolean.TRUE.equals(executedSteps.get(targetStep)) || stepFutures.containsKey(targetStep)) {
            throw new IllegalStateException("Cannot execute step " + targetStep.getName() + " because it was already attempted.");
        }
        List<String> unmetDependencies = dependencies.get(targetStep).stream()
                .filter(dependency -> !Boolean.TRUE.equals(executedSteps.get(dependency)))
                .map(FlowStep::getName)
                .toList();
        if (!unmetDependencies.isEmpty()) {
            throw new IllegalStateException("Cannot execute step " + targetStep.getName()
                    + " because it has unmet dependencies " + unmetDependencies + ".");
        }
        return executeSteps(List.of(targetStep), unitOfWork);
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
