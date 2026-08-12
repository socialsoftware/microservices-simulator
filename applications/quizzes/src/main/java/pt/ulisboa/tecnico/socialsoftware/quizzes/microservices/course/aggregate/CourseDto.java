package pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.course.aggregate;


import java.io.Serializable;

public class CourseDto implements Serializable {
    private Integer aggregateId;
    private String type;
    private String name;
    private Long version;
    private int courseQuestionCount;
    private int courseExecutionCount;

    public CourseDto() {
    }

    public CourseDto(Course course) {
        setAggregateId(course.getAggregateId());
        setType(course.getType().toString());
        setName(course.getName());
        setVersion(course.getVersion());
        setCourseQuestionCount(course.getCourseQuestionCount());
        setCourseExecutionCount(course.getCourseExecutionCount());
    }

    public Integer getAggregateId() {
        return aggregateId;
    }

    public void setAggregateId(Integer aggregateId) {
        this.aggregateId = aggregateId;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Long getVersion() {
        return version;
    }

    public void setVersion(Long version) {
        this.version = version;
    }

    public int getCourseQuestionCount() {
        return courseQuestionCount;
    }

    public void setCourseQuestionCount(int courseQuestionCount) {
        this.courseQuestionCount = courseQuestionCount;
    }

    public int getCourseExecutionCount() {
        return courseExecutionCount;
    }

    public void setCourseExecutionCount(int courseExecutionCount) {
        this.courseExecutionCount = courseExecutionCount;
    }
}
