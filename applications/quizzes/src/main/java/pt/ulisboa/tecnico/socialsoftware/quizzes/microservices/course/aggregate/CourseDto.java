package pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.course.aggregate;


import java.io.Serializable;

public class CourseDto implements Serializable {
    private Integer aggregateId;
    private String type;
    private String name;
    private Integer version;

    public CourseDto() {
    }

    public CourseDto(Course course) {
        setAggregateId(course.getAggregateId());
        setType(course.getType().toString());
        setName(course.getName());
        setVersion(course.getVersion());
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

    public Integer getVersion() {
        return version;
    }

    public void setVersion(Integer version) {
        this.version = version;
    }
}
