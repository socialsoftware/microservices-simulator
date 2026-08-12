package pt.ulisboa.tecnico.socialsoftware.answers.shared.dtos;

import java.io.Serializable;
import pt.ulisboa.tecnico.socialsoftware.answers.microservices.question.aggregate.QuestionCourse;

public class QuestionCourseDto implements Serializable {
    private String name;
    private Integer aggregateId;
    private Integer version;
    private String state;

    public QuestionCourseDto() {
    }

    public QuestionCourseDto(QuestionCourse questionCourse) {
        this.name = questionCourse.getCourseName();
        this.aggregateId = questionCourse.getCourseAggregateId();
        this.version = questionCourse.getCourseVersion();
        this.state = questionCourse.getCourseState() != null ? questionCourse.getCourseState().name() : null;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
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