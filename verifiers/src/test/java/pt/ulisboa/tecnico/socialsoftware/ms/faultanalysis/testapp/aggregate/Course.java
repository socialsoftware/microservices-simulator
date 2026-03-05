package pt.ulisboa.tecnico.socialsoftware.ms.faultanalysis.testapp.aggregate;

public class Course {
    private Integer aggregateId;

    public Course() {}

    public Course(Integer aggregateId) {
        this.aggregateId = aggregateId;
    }

    public Integer getAggregateId() {
        return aggregateId;
    }
}
