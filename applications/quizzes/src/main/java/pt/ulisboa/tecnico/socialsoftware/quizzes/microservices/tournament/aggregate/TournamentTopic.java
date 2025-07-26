package pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.tournament.aggregate;

import jakarta.persistence.*;
import pt.ulisboa.tecnico.socialsoftware.ms.domain.aggregate.Aggregate;
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.topic.aggregate.TopicDto;

@Entity
public class TournamentTopic {
    @Id
    @GeneratedValue
    private Long id;
    private Integer topicAggregateId;
    private String topicName;
    private Integer topicCourseAggregateId;
    private Integer topicVersion;
    @Enumerated(EnumType.STRING)
    private Aggregate.AggregateState state;
    @ManyToOne
    private Tournament tournament;

    public TournamentTopic() {

    }
    public TournamentTopic(TopicDto topicDto) {
        setTopicAggregateId(topicDto.getAggregateId());
        setTopicVersion(topicDto.getVersion());
        setTopicName(topicDto.getName());
        setTopicCourseAggregateId(topicDto.getCourseId());
        setState(Aggregate.AggregateState.ACTIVE);
    }

    public TournamentTopic(TournamentTopic other) {
        setTopicAggregateId(other.getTopicAggregateId());
        setTopicVersion(other.getTopicVersion());
        setTopicName(other.getTopicName());
        setTopicCourseAggregateId(other.getTopicCourseAggregateId());
        setState(other.getState());
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getId() {
        return id;
    }

    public Integer getTopicAggregateId() {
        return topicAggregateId;
    }

    public void setTopicAggregateId(Integer id) {
        this.topicAggregateId = id;
    }

    public String getTopicName() {
        return topicName;
    }

    public void setTopicName(String topicName) {
        this.topicName = topicName;
    }

    public Integer getTopicCourseAggregateId() {
        return topicCourseAggregateId;
    }

    public void setTopicCourseAggregateId(Integer courseId) {
        this.topicCourseAggregateId = courseId;
    }

    public Integer getTopicVersion() {
        return topicVersion;
    }

    public void setTopicVersion(Integer topicVersion) {
        this.topicVersion = topicVersion;
    }

    public Aggregate.AggregateState getState() {
        return state;
    }

    public void setState(Aggregate.AggregateState state) {
        this.state = state;
    }

    public Tournament getTournament() {
        return tournament;
    }

    public void setTournament(Tournament tournament) {
        this.tournament = tournament;
    }

    public TopicDto buildDto() {
        TopicDto topicDto = new TopicDto();
        topicDto.setAggregateId(getTopicAggregateId());
        topicDto.setVersion(getTopicVersion());
        topicDto.setName(getTopicName());
        topicDto.setState(getState());
        return topicDto;
    }

    @Override
    public boolean equals(Object obj) {
        if(!(obj instanceof TournamentTopic tournamentTopic)) {
            return false;
        }

        return getTopicAggregateId() != null && getTopicAggregateId().equals(tournamentTopic.getTopicAggregateId()) &&
               getTopicVersion() != null && getTopicVersion().equals(tournamentTopic.getTopicVersion());
    }

    @Override
    public int hashCode() {
        int hash = 7;
        hash = 31 * hash + getTopicAggregateId();
        hash = 31 * hash + (getTopicVersion() == null ? 0 : getTopicVersion().hashCode());
        return hash;
    }
}
