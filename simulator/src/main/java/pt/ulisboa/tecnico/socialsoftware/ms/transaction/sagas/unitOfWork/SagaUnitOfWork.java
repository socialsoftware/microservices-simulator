package pt.ulisboa.tecnico.socialsoftware.ms.transaction.sagas.unitOfWork;

import com.fasterxml.jackson.annotation.JsonProperty;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Profile;
import pt.ulisboa.tecnico.socialsoftware.ms.monitoring.TraceManager;
import pt.ulisboa.tecnico.socialsoftware.ms.transaction.sagas.aggregate.SagaAggregate.SagaState;
import pt.ulisboa.tecnico.socialsoftware.ms.transaction.unitOfWork.UnitOfWork;

import java.util.*;

@Profile("sagas")
public class SagaUnitOfWork extends UnitOfWork {

    private static final Logger logger = LoggerFactory.getLogger(SagaUnitOfWork.class);

    private final Map<String, CompensatingAction> compensatingActions;
    private final Map<Integer, String> aggregatesInSaga; // aggregateId -> aggregateType
    private final Map<String, List<AggregateStateRecord>> previousStates;
    private final List<String> executedSteps;
    private final Set<String> abortedSteps;
    private String currentExecutingStep;
    @JsonProperty
    private boolean executingCompensation;

    private final TraceManager traceManager;

    public SagaUnitOfWork(Long version, String functionalityName) {
        super(version, functionalityName);
        this.compensatingActions = new LinkedHashMap<>();
        this.aggregatesInSaga = new LinkedHashMap<>();
        this.previousStates = new LinkedHashMap<>();
        this.executedSteps = new ArrayList<>();
        this.abortedSteps = new HashSet<>();
        this.traceManager = TraceManager.getInstance();
    }

    public void registerCompensation(String stepName, Runnable compensationAction) {
        this.compensatingActions.put(stepName, new CompensatingAction(stepName, compensationAction));
    }

    public void reset() {
        this.executedSteps.clear();
        this.abortedSteps.clear();
        this.previousStates.clear();
        this.compensatingActions.clear();
        this.aggregatesInSaga.clear();
        this.currentExecutingStep = null;
        this.executingCompensation = false;
    }

    public void registerCompensation(Runnable compensationAction) {
        registerCompensation(null, compensationAction);
    }

    public void compensateStep(String stepName) {
        compensateStepForExecutor(stepName);
    }

    public boolean compensateStepForExecutor(String stepName) {
        CompensatingAction action = this.compensatingActions.get(stepName);
        if (action == null || action.isExecuted()) {
            return false;
        }
        this.traceManager.startSpanForCompensation(this.getFunctionalityName());
        logger.info("COMPENSATE: {} for step {}", action.getAction().getClass().getSimpleName(), stepName);
        boolean previousCompensationExecution = executingCompensation;
        executingCompensation = true;
        try {
            action.getAction().run();
            action.setExecuted(true);
            return true;
        } finally {
            executingCompensation = previousCompensationExecution;
            this.traceManager.endSpanForCompensation(this.getFunctionalityName());
        }
    }

    public boolean isCompensationExecuted(String stepName) {
        CompensatingAction action = this.compensatingActions.get(stepName);
        return action != null && action.isExecuted();
    }

    public boolean hasPendingCompensation(String stepName) {
        CompensatingAction action = this.compensatingActions.get(stepName);
        return action != null && !action.isExecuted();
    }

    public boolean hasPendingImplicitRollback(String stepName) {
        List<AggregateStateRecord> records = this.previousStates.get(stepName);
        return !isStepAborted(stepName) && records != null && !records.isEmpty();
    }


    public List<String> getExecutedSteps() {
        return this.executedSteps;
    }

    public String getCurrentExecutingStep() {
        return currentExecutingStep;
    }

    public void setCurrentExecutingStep(String currentExecutingStep) {
        this.currentExecutingStep = currentExecutingStep;
    }

    public Map<Integer, String> getAggregatesInSaga() {
        return this.aggregatesInSaga;
    }

    public void addToAggregatesInSaga(Integer aggregateId, String aggregateType) {
        this.aggregatesInSaga.put(aggregateId, aggregateType);
    }

    public Map<String, List<AggregateStateRecord>> getPreviousStates() {
        return this.previousStates;
    }

    public void savePreviousState(Integer aggregateId, SagaState previousState) {
        // Recovery commands still change state, but must not extend forward undo history.
        // Otherwise an unlock is recorded against the last forward step, or reintroduces
        // its own pre-unlock state when that history is subsequently restored.
        if (executingCompensation) return;
        this.previousStates.computeIfAbsent(this.currentExecutingStep, k -> new ArrayList<>())
                .add(new AggregateStateRecord(aggregateId, previousState));
    }

    public boolean isStepAborted(String stepName) {
        return this.abortedSteps.contains(stepName);
    }

    public void setStepAborted(String stepName) {
        if (stepName != null) {
            this.abortedSteps.add(stepName);
        }
    }

    public record AggregateStateRecord(Integer aggregateId, SagaState state) {}

    public static class CompensatingAction {
        private final String stepName;
        private final Runnable action;
        private boolean executed = false;

        public CompensatingAction(String stepName, Runnable action) {
            this.stepName = stepName;
            this.action = action;
        }

        public String getStepName() {
            return stepName;
        }

        public Runnable getAction() {
            return action;
        }

        public boolean isExecuted() {
            return executed;
        }

        public void setExecuted(boolean executed) {
            this.executed = executed;
        }
    }
}
