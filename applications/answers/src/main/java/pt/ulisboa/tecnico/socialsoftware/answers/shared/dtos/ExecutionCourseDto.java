package pt.ulisboa.tecnico.socialsoftware.answers.shared.dtos;

import java.io.Serializable;
import pt.ulisboa.tecnico.socialsoftware.answers.microservices.execution.aggregate.ExecutionCourse;

public class ExecutionCourseDto implements Serializable {
    private Integer aggregateId;
    private String name;
    private String type;
    private Integer version;

    public ExecutionCourseDto() {
    }

    public ExecutionCourseDto(ExecutionCourse executionCourse) {
        this.aggregateId = executionCourse.getCourseAggregateId();
        this.name = executionCourse.getCourseName();
        this.type = executionCourse.getCourseType() != null ? executionCourse.getCourseType().name() : null;
        this.version = executionCourse.getCourseVersion();
    }

    public Integer getAggregateId() {
        return aggregateId;
    }

    public void setAggregateId(Integer aggregateId) {
        this.aggregateId = aggregateId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public Integer getVersion() {
        return version;
    }

    public void setVersion(Integer version) {
        this.version = version;
    }
}