package pt.ulisboa.tecnico.socialsoftware.ms.coordination.workflow;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import pt.ulisboa.tecnico.socialsoftware.ms.coordination.unitOfWork.UnitOfWork;
import pt.ulisboa.tecnico.socialsoftware.ms.coordination.unitOfWork.UnitOfWorkService;
import pt.ulisboa.tecnico.socialsoftware.ms.quizzes.microservices.exception.TutorException;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.workflow.SagaSyncStep;

public abstract class Workflow {
    private static final Logger logger = LoggerFactory.getLogger(SagaSyncStep.class);

    protected UnitOfWorkService unitOfWorkService;
    private UnitOfWork unitOfWork;
    private WorkflowFunctionality functionality;
    protected HashMap<FlowStep, ArrayList<FlowStep>> stepsWithDependencies = new HashMap<>();
    private ExecutionPlan executionPlan; // redefined for each transaction model
    private HashMap<String, FlowStep> stepNameMap = new HashMap<>();

    public Workflow(WorkflowFunctionality functionality, UnitOfWorkService unitOfWorkService, UnitOfWork unitOfWork) {
        this.functionality = functionality;
        this.unitOfWorkService = unitOfWorkService;
        this.unitOfWork = unitOfWork;
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

    public void addStep(FlowStep step){
        this.stepsWithDependencies.put(step, step.getDependencies());
        stepNameMap.put(step.getName(), step);
    }
    
    public void executeUntilStep(String stepName, UnitOfWork unitOfWork) {
        logger.info("EXECUTE FUNCTIONALITY: {} with version {} until step {}", unitOfWork.getFunctionalityName(), unitOfWork.getVersion(), stepName);

        if (this.executionPlan == null) {
            this.executionPlan = planOrder(this.stepsWithDependencies);
        }
        
        FlowStep targetStep = getStepByName(stepName);
        executionPlan.executeUntilStep(targetStep, unitOfWork).join();
    }

    public CompletableFuture<Void> resume(UnitOfWork unitOfWork) {
        logger.info("EXECUTE FUNCTIONALITY: {} with version {} until end", unitOfWork.getFunctionalityName(), unitOfWork.getVersion());
        try {
            return executionPlan.resume(unitOfWork)
                .thenRun(() -> {
                    unitOfWorkService.commit(unitOfWork);
                })
                .exceptionally(ex -> {
                    Throwable cause = (ex instanceof CompletionException) ? ex.getCause() : ex;
    
                    unitOfWorkService.abort(unitOfWork);
    
                    if (cause instanceof TutorException) {
                        throw (TutorException) cause;
                    } else {
                        throw new RuntimeException(cause);  
                    }
                });
        } catch (TutorException e) {
            unitOfWorkService.abort(unitOfWork);
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

        this.executionPlan = planOrder(this.stepsWithDependencies);
        try {
            return executionPlan.execute(unitOfWork)
                .thenRun(() -> {
                    unitOfWorkService.commit(unitOfWork);
                    logger.info("END EXECUTION FUNCTIONALITY: {} with version {}", unitOfWork.getFunctionalityName(), unitOfWork.getVersion());

                })
                .exceptionally(ex -> {
                    Throwable cause = (ex instanceof CompletionException) ? ex.getCause() : ex;
    
                    unitOfWorkService.abort(unitOfWork);
                    logger.info("ABORT EXECUTION FUNCTIONALITY: {} with version {}", unitOfWork.getFunctionalityName(), unitOfWork.getVersion());

                    if (cause instanceof TutorException) {
                        throw (TutorException) cause;
                    } else {
                        throw new RuntimeException(cause);  
                    }
                });
        } catch (TutorException e) {
            unitOfWorkService.abort(unitOfWork);
            throw e;
        }
    }
}
