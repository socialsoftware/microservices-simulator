package pt.ulisboa.tecnico.socialsoftware.quizzesfull.microservices.course.aggregate;

import java.io.Serializable;

public class CourseDto implements Serializable {

    private Integer aggregateId;
    private String name;
    private String type;
    private Long version;
    private int executionCount;
    private int questionCount;

    public CourseDto() {}

    public CourseDto(Course course) {
        setAggregateId(course.getAggregateId());
        setName(course.getName());
        setType(course.getType().toString());
        setVersion(course.getVersion());
        setExecutionCount(course.getExecutionCount());
        setQuestionCount(course.getQuestionCount());
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

    public Long getVersion() {
        return version;
    }

    public void setVersion(Long version) {
        this.version = version;
    }

    public int getExecutionCount() {
        return executionCount;
    }

    public void setExecutionCount(int executionCount) {
        this.executionCount = executionCount;
    }

    public int getQuestionCount() {
        return questionCount;
    }

    public void setQuestionCount(int questionCount) {
        this.questionCount = questionCount;
    }
}
