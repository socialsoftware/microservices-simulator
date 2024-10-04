package pt.ulisboa.tecnico.socialsoftware.ms.sagas.aggregate;

public interface SagaAggregate {
    void setSagaState(SagaState state);
    SagaState getSagaState();

    public interface SagaState {
        String getStateName();
    }
}