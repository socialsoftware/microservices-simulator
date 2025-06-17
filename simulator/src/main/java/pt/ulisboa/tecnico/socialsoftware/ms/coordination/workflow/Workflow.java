package pt.ulisboa.tecnico.socialsoftware.ms.coordination.workflow;

import java.io.File;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import pt.ulisboa.tecnico.socialsoftware.ms.coordination.unitOfWork.UnitOfWork;
import pt.ulisboa.tecnico.socialsoftware.ms.coordination.unitOfWork.UnitOfWorkService;
import pt.ulisboa.tecnico.socialsoftware.ms.exception.SimulatorException;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.workflow.SagaSyncStep;
import pt.ulisboa.tecnico.socialsoftware.ms.utils.TraceManager;

public abstract class Workflow {
    private static final Logger logger = LoggerFactory.getLogger(SagaSyncStep.class);

    protected UnitOfWorkService unitOfWorkService;
    private UnitOfWork unitOfWork;
    private WorkflowFunctionality functionality;
    protected HashMap<FlowStep, ArrayList<FlowStep>> stepsWithDependencies = new HashMap<>();
    private ExecutionPlan executionPlan; // redefined for each transaction model
    private HashMap<String, FlowStep> stepNameMap = new HashMap<>();
    private TraceManager traceManager;

    public Workflow(WorkflowFunctionality functionality, UnitOfWorkService unitOfWorkService, UnitOfWork unitOfWork) {
        this.functionality = functionality;
        this.unitOfWorkService = unitOfWorkService;
        this.unitOfWork = unitOfWork;
        this.traceManager = TraceManager.getInstance();
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

    public int getWorkflowTotalDelay() {
        return this.executionPlan.getTotalDelay();
    }

    public void addStep(FlowStep step){
        this.stepsWithDependencies.put(step, step.getDependencies());
        stepNameMap.put(step.getName(), step);
    }
    
    public void executeUntilStep(String stepName, UnitOfWork unitOfWork) {
         logger.info("START EXECUTION FUNCTIONALITY: {} with version {}", unitOfWork.getFunctionalityName(), unitOfWork.getVersion());
         this.traceManager.startSpanForFunctionality(unitOfWork.getFunctionalityName()); 

        if (this.executionPlan == null) {
            this.executionPlan = planOrder(this.stepsWithDependencies);
        }
        
        FlowStep targetStep = getStepByName(stepName);
        executionPlan.executeUntilStep(targetStep, unitOfWork).join();
    }

    public CompletableFuture<Void> resume(UnitOfWork unitOfWork) {
        try {
            return executionPlan.resume(unitOfWork)
                .thenRun(() -> {
                    unitOfWorkService.commit(unitOfWork);
                    logger.info("END EXECUTION FUNCTIONALITY: {} with version {}", unitOfWork.getFunctionalityName(), unitOfWork.getVersion());
                    this.traceManager.endSpanForFunctionality(unitOfWork.getFunctionalityName());
                })
                .exceptionally(ex -> {
                    Throwable cause = (ex instanceof CompletionException) ? ex.getCause() : ex;
    
                    unitOfWorkService.abort(unitOfWork);
                    logger.info("ABORT EXECUTION FUNCTIONALITY: {} with version {}", unitOfWork.getFunctionalityName(), unitOfWork.getVersion());
    
                    if (cause instanceof SimulatorException) {
                        throw (SimulatorException) cause;
                    } else {
                        throw new RuntimeException(cause);  
                    }
                });
        } catch (SimulatorException e) {
            unitOfWorkService.abort(unitOfWork);
            logger.info("ABORT EXECUTION FUNCTIONALITY: {} with version {}", unitOfWork.getFunctionalityName(), unitOfWork.getVersion());
            this.traceManager.endSpanForFunctionality(unitOfWork.getFunctionalityName());
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
        logger.info("START EXECUTION FUNCTIONALITY: {} with version {}", unitOfWork.getFunctionalityName(), unitOfWork.getVersion());
        this.traceManager.startSpanForFunctionality(unitOfWork.getFunctionalityName());

        this.executionPlan = planOrder(this.stepsWithDependencies);
        
        CompletableFuture<Void> executionFuture;

        try {
            
            executionFuture = executionPlan.execute(unitOfWork);

            return executionFuture
                .thenRun(() -> {
                    unitOfWorkService.commit(unitOfWork);
                    logger.info("END EXECUTION FUNCTIONALITY: {} with version {}", unitOfWork.getFunctionalityName(), unitOfWork.getVersion());
                    this.traceManager.endSpanForFunctionality(unitOfWork.getFunctionalityName());
                })
                .exceptionally(ex -> {
                    Throwable cause = (ex instanceof CompletionException) ? ex.getCause() : ex;
    
                    unitOfWorkService.abort(unitOfWork);
                    logger.info("ABORT EXECUTION FUNCTIONALITY: {} with version {}", unitOfWork.getFunctionalityName(), unitOfWork.getVersion());
                    
                    if (cause instanceof SimulatorException) {
                        throw (SimulatorException) cause;
                    } else {
                        throw new RuntimeException(cause);  
                    }
                });
        } catch (SimulatorException e) {
            unitOfWorkService.abort(unitOfWork);
            logger.info("ABORT EXECUTION FUNCTIONALITY: {} with version {}", unitOfWork.getFunctionalityName(), unitOfWork.getVersion());
            this.traceManager.endSpanForFunctionality(unitOfWork.getFunctionalityName());
            throw e;
        }
    } 
}