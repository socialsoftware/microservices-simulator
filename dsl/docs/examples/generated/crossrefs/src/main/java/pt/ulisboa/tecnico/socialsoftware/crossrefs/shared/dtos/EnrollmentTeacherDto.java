package pt.ulisboa.tecnico.socialsoftware.crossrefs.shared.dtos;

import java.io.Serializable;
import pt.ulisboa.tecnico.socialsoftware.crossrefs.microservices.enrollment.aggregate.EnrollmentTeacher;

public class EnrollmentTeacherDto implements Serializable {
    private Integer aggregateId;
    private Integer version;
    private String state;

    public EnrollmentTeacherDto() {
    }

    public EnrollmentTeacherDto(EnrollmentTeacher enrollmentTeacher) {
        this.aggregateId = enrollmentTeacher.getTeacherAggregateId();
        this.version = enrollmentTeacher.getTeacherVersion();
        this.state = enrollmentTeacher.getTeacherState() != null ? enrollmentTeacher.getTeacherState().name() : null;
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

    public String getState() {
        return state;
    }

    public void setState(String state) {
        this.state = state;
    }
}