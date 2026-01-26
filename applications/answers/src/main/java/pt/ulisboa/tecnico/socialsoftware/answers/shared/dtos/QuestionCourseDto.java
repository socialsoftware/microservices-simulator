package pt.ulisboa.tecnico.socialsoftware.answers.shared.dtos;

import java.io.Serializable;
import pt.ulisboa.tecnico.socialsoftware.answers.microservices.question.aggregate.QuestionCourse;

public class QuestionCourseDto implements Serializable {
    private Integer aggregateId;
    private String name;
    private Integer version;

    public QuestionCourseDto() {
    }

    public QuestionCourseDto(QuestionCourse questionCourse) {
        this.aggregateId = questionCourse.getCourseAggregateId();
        this.name = questionCourse.getCourseName();
        this.version = questionCourse.getCourseVersion();
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

    public Integer getVersion() {
        return version;
    }

    public void setVersion(Integer version) {
        this.version = version;
    }
}