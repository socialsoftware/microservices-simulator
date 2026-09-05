package pt.ulisboa.tecnico.socialsoftware.quizzesfull2.microservices.topic.aggregate;

public class TopicDto {
    private Integer aggregateId;
    private Long version;
    private String name;
    private Integer courseAggregateId;

    public TopicDto() {
    }

    public TopicDto(Integer aggregateId, Long version, String name, Integer courseAggregateId) {
        this.aggregateId = aggregateId;
        this.version = version;
        this.name = name;
        this.courseAggregateId = courseAggregateId;
    }

    public TopicDto(Topic topic) {
        this.aggregateId = topic.getAggregateId();
        this.version = topic.getVersion();
        this.name = topic.getName();
        this.courseAggregateId = topic.getCourseAggregateId();
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

    public Integer getCourseAggregateId() {
        return courseAggregateId;
    }

    public void setCourseAggregateId(Integer courseAggregateId) {
        this.courseAggregateId = courseAggregateId;
    }
}
