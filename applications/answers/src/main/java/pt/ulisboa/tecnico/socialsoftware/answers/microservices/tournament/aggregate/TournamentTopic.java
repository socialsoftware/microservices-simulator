package pt.ulisboa.tecnico.socialsoftware.answers.microservices.tournament.aggregate;

import jakarta.persistence.Embeddable;
import pt.ulisboa.tecnico.socialsoftware.ms.coordination.unitOfWork.UnitOfWork;
import pt.ulisboa.tecnico.socialsoftware.ms.domain.aggregate.Aggregate.AggregateState;

@Embeddable
public class TournamentTopic {
    private Long id;
    private Integer topicAggregateId;
    private String topicName;
    private Integer topicCourseAggregateId;
    private Integer topicVersion;
    private AggregateState state;
    private Object tournament; 

    public TournamentTopic(Long id, Integer topicAggregateId, String topicName, Integer topicCourseAggregateId, Integer topicVersion, AggregateState state, Object tournament) {
        this.id = id;
        this.topicAggregateId = topicAggregateId;
        this.topicName = topicName;
        this.topicCourseAggregateId = topicCourseAggregateId;
        this.topicVersion = topicVersion;
        this.state = state;
        this.tournament = tournament;
    }

    public TournamentTopic(TournamentTopic other) {
        // Copy constructor
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

    public Integer getTopicVersion() {
        return topicVersion;
    }

    public void setTopicVersion(Integer topicVersion) {
        this.topicVersion = topicVersion;
    }

    public AggregateState getState() {
        return state;
    }

    public void setState(AggregateState state) {
        this.state = state;
    }

    public Object getTournament() {
        return tournament;
    }

    public void setTournament(Object tournament) {
        this.tournament = tournament;
    }


}