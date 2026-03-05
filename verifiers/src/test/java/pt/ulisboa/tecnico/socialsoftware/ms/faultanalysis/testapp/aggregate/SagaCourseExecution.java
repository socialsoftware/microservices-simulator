package pt.ulisboa.tecnico.socialsoftware.ms.faultanalysis.testapp.aggregate;

public class SagaCourseExecution extends CourseExecution {
    private String sagaState;

    public SagaCourseExecution() {}

    public SagaCourseExecution(Integer aggregateId) {
        super(aggregateId);
    }

    public String getSagaState() {
        return sagaState;
    }

    public void setSagaState(String sagaState) {
        this.sagaState = sagaState;
    }
}
