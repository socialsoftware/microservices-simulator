package pt.ulisboa.tecnico.socialsoftware.ms.faultanalysis.testapp.aggregate;

public class CourseExecution {
    private Integer aggregateId;

    public CourseExecution() {}

    public CourseExecution(Integer aggregateId) {
        this.aggregateId = aggregateId;
    }

    public Integer getAggregateId() {
        return aggregateId;
    }
}
