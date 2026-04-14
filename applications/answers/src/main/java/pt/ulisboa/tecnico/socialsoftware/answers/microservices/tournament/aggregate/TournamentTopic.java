package pt.ulisboa.tecnico.socialsoftware.answers.microservices.tournament.aggregate;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;

import pt.ulisboa.tecnico.socialsoftware.ms.domain.aggregate.Aggregate.AggregateState;

import pt.ulisboa.tecnico.socialsoftware.answers.shared.dtos.TopicDto;
import pt.ulisboa.tecnico.socialsoftware.answers.shared.dtos.TournamentTopicDto;

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
    @ManyToOne
    private Tournament tournament;

    public TournamentTopic() {

    }

    public TournamentTopic(TopicDto topicDto) {
        setTopicAggregateId(topicDto.getAggregateId());
        setTopicVersion(topicDto.getVersion());
        setTopicState(topicDto.getState());
    }

    public TournamentTopic(TournamentTopicDto tournamentTopicDto) {
        setTopicAggregateId(tournamentTopicDto.getAggregateId());
        setTopicVersion(tournamentTopicDto.getVersion());
        setTopicState(tournamentTopicDto.getState() != null ? AggregateState.valueOf(tournamentTopicDto.getState()) : null);
        setTopicName(tournamentTopicDto.getName());
        setTopicCourseAggregateId(tournamentTopicDto.getCourseAggregateId());
    }

    public TournamentTopic(TournamentTopic other) {
        setTopicAggregateId(other.getTopicAggregateId());
        setTopicVersion(other.getTopicVersion());
        setTopicState(other.getTopicState());
        setTopicName(other.getTopicName());
        setTopicCourseAggregateId(other.getTopicCourseAggregateId());
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
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




    public TournamentTopicDto buildDto() {
        TournamentTopicDto dto = new TournamentTopicDto();
        dto.setAggregateId(getTopicAggregateId());
        dto.setVersion(getTopicVersion());
        dto.setState(getTopicState() != null ? getTopicState().name() : null);
        dto.setName(getTopicName());
        dto.setCourseAggregateId(getTopicCourseAggregateId());
        return dto;
    }
}