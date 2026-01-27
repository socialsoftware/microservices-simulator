package pt.ulisboa.tecnico.socialsoftware.answers.shared.dtos;

import java.io.Serializable;
import pt.ulisboa.tecnico.socialsoftware.answers.microservices.question.aggregate.QuestionCourse;

public class QuestionCourseDto implements Serializable {
    private Integer aggregateId;
    private Integer version;
    private String name;

    public QuestionCourseDto() {
    }

    public QuestionCourseDto(QuestionCourse questionCourse) {
        this.aggregateId = questionCourse.getCourseAggregateId();
        this.version = questionCourse.getCourseVersion();
        this.name = questionCourse.getCourseName();
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
}