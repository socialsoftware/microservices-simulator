package pt.ulisboa.tecnico.socialsoftware.ms.sagas.aggregate;

import pt.ulisboa.tecnico.socialsoftware.ms.domain.aggregate.MergeableAggregate;

public interface SagaAggregate extends MergeableAggregate {
    void setSagaState(SagaState state);
    SagaState getSagaState();

    public interface SagaState {
        String getStateName();
    }
}