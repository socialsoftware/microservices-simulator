package pt.ulisboa.tecnico.socialsoftware.crossrefs.shared.dtos;

import java.io.Serializable;
import pt.ulisboa.tecnico.socialsoftware.ms.domain.aggregate.Aggregate.AggregateState;
import pt.ulisboa.tecnico.socialsoftware.crossrefs.microservices.enrollment.aggregate.EnrollmentTeacher;

public class EnrollmentTeacherDto implements Serializable {
    private Integer aggregateId;
    private Integer version;
    private AggregateState state;

    public EnrollmentTeacherDto() {
    }

    public EnrollmentTeacherDto(EnrollmentTeacher enrollmentTeacher) {
        this.aggregateId = enrollmentTeacher.getTeacherAggregateId();
        this.version = enrollmentTeacher.getTeacherVersion();
        this.state = enrollmentTeacher.getTeacherState();
    }

    public Integer getAggregateId() {
        return aggregateId;
    }

    public void setAggregateId(Integer aggregateId) {
        this.aggregateId = aggregateId;
    }

    public Integer getVersion() {
        return version;
    }

    public void setVersion(Integer version) {
        this.version = version;
    }

    public AggregateState getState() {
        return state;
    }

    public void setState(AggregateState state) {
        this.state = state;
    }
}