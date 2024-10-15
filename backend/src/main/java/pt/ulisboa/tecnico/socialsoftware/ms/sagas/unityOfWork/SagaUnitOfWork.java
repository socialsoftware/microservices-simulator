package pt.ulisboa.tecnico.socialsoftware.ms.sagas.unityOfWork;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.stream.Collectors;

import org.springframework.context.annotation.Profile;

import pt.ulisboa.tecnico.socialsoftware.ms.coordination.unitOfWork.UnitOfWork;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.aggregate.SagaAggregate;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.aggregate.SagaAggregate.SagaState;

@Profile("sagas")
public class SagaUnitOfWork extends UnitOfWork {
    private ArrayList<Runnable> compensatingActions;
    private ArrayList<SagaAggregate> aggregatesInSaga;
    private HashMap<Integer, SagaState> previousStates = new HashMap<>();

    public SagaUnitOfWork(Integer version, String functionalityName) {
        super(version, functionalityName);
        this.compensatingActions = new ArrayList<>();
        this.aggregatesInSaga = new ArrayList<>();
    }

    public void registerCompensation(Runnable compensationAction) {
        this.compensatingActions.add(compensationAction);
    }

    public void compensate() {
        Collections.reverse(this.compensatingActions);
        for (Runnable action: compensatingActions) {
            action.run();
        }
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

    public ArrayList<SagaAggregate> getAggregatesInSaga() {
        return this.aggregatesInSaga;
    }

    public void addToAggregatesInSaga(SagaAggregate aggregate) {
        this.aggregatesInSaga.add(aggregate);
    }

    public HashMap<Integer, SagaState> getPreviousStates() {
        return this.previousStates;
    }

    public void savePreviousState(Integer aggregateId, SagaState previousState) {
        this.previousStates.put(aggregateId, previousState);
    }
}
