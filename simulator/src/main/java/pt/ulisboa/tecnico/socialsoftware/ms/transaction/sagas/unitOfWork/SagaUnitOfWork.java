package pt.ulisboa.tecnico.socialsoftware.ms.transaction.sagas.unitOfWork;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Profile;
import pt.ulisboa.tecnico.socialsoftware.ms.monitoring.TraceManager;
import pt.ulisboa.tecnico.socialsoftware.ms.transaction.sagas.aggregate.SagaAggregate.SagaState;
import pt.ulisboa.tecnico.socialsoftware.ms.transaction.unitOfWork.UnitOfWork;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.stream.Collectors;

@Profile("sagas")
public class SagaUnitOfWork extends UnitOfWork {

    private static final Logger logger = LoggerFactory.getLogger(SagaUnitOfWork.class);

    private final ArrayList<CompensatingAction> compensatingActions;
    private final Map<Integer, String> aggregatesInSaga; // aggregateId -> aggregateType
    private final HashMap<Integer, SagaState> previousStates = new HashMap<>();
    private boolean abortCommandsSent = false;

    private final TraceManager traceManager;

    public SagaUnitOfWork(Long version, String functionalityName) {
        super(version, functionalityName);
        this.compensatingActions = new ArrayList<>();
        this.aggregatesInSaga = new LinkedHashMap<>();
        this.traceManager = TraceManager.getInstance();
    }

    public void registerCompensation(String stepName, Runnable compensationAction) {
        this.compensatingActions.add(new CompensatingAction(stepName, compensationAction));
    }

    public void registerCompensation(Runnable compensationAction) {
        registerCompensation(null, compensationAction);
    }

    public void compensate() {
        List<CompensatingAction> toExecute = new ArrayList<>(this.compensatingActions);
        Collections.reverse(toExecute);
        this.traceManager.startSpanForCompensation(this.getFunctionalityName());
        for (CompensatingAction action : toExecute) {
            if (!action.isExecuted()) {
                logger.info("COMPENSATE: {}", action.getAction().getClass().getSimpleName());
                action.getAction().run();
                action.setExecuted(true);
            }
        }
        this.traceManager.endSpanForCompensation(this.getFunctionalityName());
    }

    public void compensateUntilStep(String targetStepName) {
        List<CompensatingAction> toExecute = new ArrayList<>(this.compensatingActions);
        Collections.reverse(toExecute);
        this.traceManager.startSpanForCompensation(this.getFunctionalityName());
        for (CompensatingAction action : toExecute) {
            if (!action.isExecuted()) {
                logger.info("COMPENSATE: {} for step {}", action.getAction().getClass().getSimpleName(), action.getStepName());
                action.getAction().run();
                action.setExecuted(true);
            }
            if (action.getStepName() != null && action.getStepName().equals(targetStepName)) {
                break;
            }
        }
        this.traceManager.endSpanForCompensation(this.getFunctionalityName());
    }

    public CompletableFuture<Void> compensateAsync(ExecutorService executorService) {
        // Reverse the compensating actions
        List<CompensatingAction> toExecute = new ArrayList<>(this.compensatingActions);
        Collections.reverse(toExecute);

        // Execute compensations asynchronously using the provided ExecutorService
        List<CompletableFuture<Void>> compensationFutures = toExecute.stream()
                .filter(action -> !action.isExecuted())
                .map(action -> CompletableFuture.runAsync(() -> {
                    action.getAction().run();
                    action.setExecuted(true);
                }, executorService))
                .collect(Collectors.toList());

        // Combine all compensation futures into a single CompletableFuture
        return CompletableFuture.allOf(compensationFutures.toArray(new CompletableFuture[0]));
    }

    public Map<Integer, String> getAggregatesInSaga() {
        return this.aggregatesInSaga;
    }

    public void addToAggregatesInSaga(Integer aggregateId, String aggregateType) {
        this.aggregatesInSaga.put(aggregateId, aggregateType);
    }

    public HashMap<Integer, SagaState> getPreviousStates() {
        return this.previousStates;
    }

    public void savePreviousState(Integer aggregateId, SagaState previousState) {
        this.previousStates.put(aggregateId, previousState);
    }

    public boolean isAbortCommandsSent() {
        return abortCommandsSent;
    }

    public void setAbortCommandsSent(boolean abortCommandsSent) {
        this.abortCommandsSent = abortCommandsSent;
    }

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
