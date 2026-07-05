package pt.ulisboa.tecnico.socialsoftware.ms.transaction.sagas.aggregate;

public interface SagaAggregate {
    void setSagaState(SagaState state);

    SagaState getSagaState();

    /**
     * Returns the neutral saga state for this aggregate (for example, NOT_IN_SAGA).
     */
    SagaState getNeutralSagaState();

    public interface SagaState {
        String getStateName();
    }
}