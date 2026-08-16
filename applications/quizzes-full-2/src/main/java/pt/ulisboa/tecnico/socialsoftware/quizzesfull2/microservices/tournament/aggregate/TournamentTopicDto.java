package pt.ulisboa.tecnico.socialsoftware.quizzesfull2.microservices.tournament.aggregate;

public class TournamentTopicDto {
    private Integer topicAggregateId;
    private String topicName;
    private Long topicVersion;
    private Integer courseAggregateId;

    public TournamentTopicDto() {
    }

    public TournamentTopicDto(Integer topicAggregateId, String topicName, Long topicVersion,
                              Integer courseAggregateId) {
        this.topicAggregateId = topicAggregateId;
        this.topicName = topicName;
        this.topicVersion = topicVersion;
        this.courseAggregateId = courseAggregateId;
    }

    public TournamentTopicDto(TournamentTopic tournamentTopic) {
        this.topicAggregateId = tournamentTopic.getTopicAggregateId();
        this.topicName = tournamentTopic.getTopicName();
        this.topicVersion = tournamentTopic.getTopicVersion();
        this.courseAggregateId = tournamentTopic.getCourseAggregateId();
    }

    public Integer getTopicAggregateId() {
        return topicAggregateId;
    }

    public void setTopicAggregateId(Integer topicAggregateId) {
        this.topicAggregateId = topicAggregateId;
    }

    public String getTopicName() {
        return topicName;
    }

    public void setTopicName(String topicName) {
        this.topicName = topicName;
    }

    public Long getTopicVersion() {
        return topicVersion;
    }

    public void setTopicVersion(Long topicVersion) {
        this.topicVersion = topicVersion;
    }

    public Integer getCourseAggregateId() {
        return courseAggregateId;
    }

    public void setCourseAggregateId(Integer courseAggregateId) {
        this.courseAggregateId = courseAggregateId;
    }
}
