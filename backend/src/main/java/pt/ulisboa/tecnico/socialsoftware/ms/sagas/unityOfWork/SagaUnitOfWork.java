package pt.ulisboa.tecnico.socialsoftware.ms.sagas.unityOfWork;

import java.util.ArrayList;
import java.util.Collections;

import org.springframework.context.annotation.Profile;

import pt.ulisboa.tecnico.socialsoftware.ms.coordination.unitOfWork.UnitOfWork;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.aggregate.SagaAggregate;

@Profile("sagas")
public class SagaUnitOfWork extends UnitOfWork {
    private ArrayList<Runnable> compensatingActions;
    private ArrayList<SagaAggregate> aggregatesInSaga;

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

    public ArrayList<SagaAggregate> getAggregatesInSaga() {
        return this.aggregatesInSaga;
    }

    public void addToAggregatesInSaga(SagaAggregate aggregate) {
        this.aggregatesInSaga.add(aggregate);
    }
}
