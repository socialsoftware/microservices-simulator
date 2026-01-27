package pt.ulisboa.tecnico.socialsoftware.answers.shared.dtos;

import java.io.Serializable;
import pt.ulisboa.tecnico.socialsoftware.answers.microservices.execution.aggregate.ExecutionCourse;

public class ExecutionCourseDto implements Serializable {
    private Integer aggregateId;
    private Integer version;
    private String name;
    private String type;

    public ExecutionCourseDto() {
    }

    public ExecutionCourseDto(ExecutionCourse executionCourse) {
        this.aggregateId = executionCourse.getCourseAggregateId();
        this.version = executionCourse.getCourseVersion();
        this.name = executionCourse.getCourseName();
        this.type = executionCourse.getCourseType() != null ? executionCourse.getCourseType().name() : null;
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
}