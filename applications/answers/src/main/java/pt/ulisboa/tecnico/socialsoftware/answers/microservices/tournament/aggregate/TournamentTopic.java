package pt.ulisboa.tecnico.socialsoftware.answers.microservices.tournament.aggregate;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.OneToOne;

import pt.ulisboa.tecnico.socialsoftware.ms.domain.aggregate.Aggregate.AggregateState;

import pt.ulisboa.tecnico.socialsoftware.answers.shared.dtos.TopicDto;

@Entity
public class TournamentTopic {
    @Id
    @GeneratedValue
    private Long id;
    private Integer topicAggregateId;
    private Integer topicVersion;
    private AggregateState topicState;
    private String topicName;
    private Integer topicCourseAggregateId;
    @OneToOne
    private Tournament tournament;

    public TournamentTopic() {

    }

    public TournamentTopic(TopicDto topicDto) {
        setTopicAggregateId(topicDto.getAggregateId());
        setTopicVersion(topicDto.getVersion());
        setTopicState(topicDto.getState());
        setTopicName(topicDto.getName());
        setTopicCourseAggregateId(topicDto.getCourseAggregateId());
    }

    public TournamentTopic(TournamentTopic other) {
        setTopicVersion(other.getTopicVersion());
        setTopicState(other.getTopicState());
        setTopicName(other.getTopicName());
        setTopicCourseAggregateId(other.getTopicCourseAggregateId());
    }

    public Integer getTopicAggregateId() {
        return topicAggregateId;
    }

    public void setTopicAggregateId(Integer topicAggregateId) {
        this.topicAggregateId = topicAggregateId;
    }

    public Integer getTopicVersion() {
        return topicVersion;
    }

    public void setTopicVersion(Integer topicVersion) {
        this.topicVersion = topicVersion;
    }

    public AggregateState getTopicState() {
        return topicState;
    }

    public void setTopicState(AggregateState topicState) {
        this.topicState = topicState;
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

    public void setTopicCourseAggregateId(Integer topicCourseAggregateId) {
        this.topicCourseAggregateId = topicCourseAggregateId;
    }

    public Tournament getTournament() {
        return tournament;
    }

    public void setTournament(Tournament tournament) {
        this.tournament = tournament;
    }


    public TopicDto buildDto() {
        TopicDto dto = new TopicDto();
        dto.setAggregateId(getTopicAggregateId());
        dto.setVersion(getTopicVersion());
        dto.setState(getTopicState());
        dto.setName(getTopicName());
        dto.setCourseAggregateId(getTopicCourseAggregateId());
        return dto;
    }
}