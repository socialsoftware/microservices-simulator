package pt.ulisboa.tecnico.socialsoftware.ms.sagas.unitOfWork;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Profile;
import pt.ulisboa.tecnico.socialsoftware.ms.coordination.unitOfWork.UnitOfWork;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.aggregate.SagaAggregate;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.aggregate.SagaAggregate.SagaState;
import pt.ulisboa.tecnico.socialsoftware.ms.utils.TraceManager;

import java.io.Serial;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.stream.Collectors;

@Profile("sagas")
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonTypeInfo(use = JsonTypeInfo.Id.CLASS, property = "@class")
public class SagaUnitOfWork extends UnitOfWork {

    @Serial
    private static final long serialVersionUID = 1L;

    private static final Logger logger = LoggerFactory.getLogger(SagaUnitOfWork.class);

    private final ArrayList<Runnable> compensatingActions;
    private final ArrayList<Integer> aggregatesInSaga; //TODO CHANGE IN DISTRIBUTED
    private final HashMap<Integer, SagaState> previousStates = new HashMap<>();

    private final TraceManager traceManager;

    public SagaUnitOfWork(Integer version, String functionalityName) {
        super(version, functionalityName);
        this.compensatingActions = new ArrayList<>();
        this.aggregatesInSaga = new ArrayList<>();
        this.traceManager = TraceManager.getInstance();
    }

    public void registerCompensation(Runnable compensationAction) {
        this.compensatingActions.add(compensationAction);
    }

    public void compensate() {
        Collections.reverse(this.compensatingActions);
        this.traceManager.startSpanForCompensation(this.getFunctionalityName()); 
        for (Runnable action: compensatingActions) {
            logger.info("COMPENSATE: {}", action.getClass().getSimpleName());
            action.run();
        }
        this.traceManager.endSpanForCompensation(this.getFunctionalityName());
    }

    public CompletableFuture<Void> compensateAsync(ExecutorService executorService) {
        // Reverse the compensating actions
        Collections.reverse(this.compensatingActions);

        // Execute compensations asynchronously using the provided ExecutorService
        List<CompletableFuture<Void>> compensationFutures = compensatingActions.stream()
            .map(action -> CompletableFuture.runAsync(action, executorService))
            .collect(Collectors.toList());

        // Combine all compensation futures into a single CompletableFuture
        return CompletableFuture.allOf(compensationFutures.toArray(new CompletableFuture[0]));
    }

    public ArrayList<Integer> getAggregatesInSaga() {
        return this.aggregatesInSaga;
    }

    public void addToAggregatesInSaga(Integer aggregateId) {
        this.aggregatesInSaga.add(aggregateId);
    }

    public HashMap<Integer, SagaState> getPreviousStates() {
        return this.previousStates;
    }

    public void savePreviousState(Integer aggregateId, SagaState previousState) {
        this.previousStates.put(aggregateId, previousState);
    }
}
