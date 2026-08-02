package pt.ulisboa.tecnico.socialsoftware.quizzesfull2.microservices.course.aggregate;

public class CourseDto {
    private Integer aggregateId;
    private Long version;
    private String name;
    private CourseType type;

    public CourseDto() {
    }

    public CourseDto(Integer aggregateId, Long version, String name, CourseType type) {
        this.aggregateId = aggregateId;
        this.version = version;
        this.name = name;
        this.type = type;
    }

    public CourseDto(Course course) {
        this.aggregateId = course.getAggregateId();
        this.version = course.getVersion();
        this.name = course.getName();
        this.type = course.getType();
    }

    public Integer getAggregateId() {
        return aggregateId;
    }

    public void setAggregateId(Integer aggregateId) {
        this.aggregateId = aggregateId;
    }

    public Long getVersion() {
        return version;
    }

    public void setVersion(Long version) {
        this.version = version;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public CourseType getType() {
        return type;
    }

    public void setType(CourseType type) {
        this.type = type;
    }
}
