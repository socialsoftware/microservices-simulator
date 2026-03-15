package pt.ulisboa.tecnico.socialsoftware.ms.faultanalysis.testapp.aggregate;

public class SagaCourse extends Course {
    private String sagaState;

    public SagaCourse() {}

    public SagaCourse(Integer aggregateId) {
        super(aggregateId);
    }

    public String getSagaState() {
        return sagaState;
    }

    public void setSagaState(String sagaState) {
        this.sagaState = sagaState;
    }
}
