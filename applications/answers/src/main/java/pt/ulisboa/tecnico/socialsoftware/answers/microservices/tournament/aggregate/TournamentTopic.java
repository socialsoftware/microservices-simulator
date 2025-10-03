package pt.ulisboa.tecnico.socialsoftware.answers.microservices.tournament.aggregate;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.OneToOne;
import jakarta.persistence.CascadeType;
import java.util.stream.Collectors;
import pt.ulisboa.tecnico.socialsoftware.ms.coordination.unitOfWork.UnitOfWork;
import pt.ulisboa.tecnico.socialsoftware.ms.domain.aggregate.Aggregate.AggregateState;
import pt.ulisboa.tecnico.socialsoftware.answers.shared.dtos.TournamentDto;

@Entity
public class TournamentTopic {
    @Id
    @GeneratedValue
    private Long id;
    private Integer topicAggregateId;
    private String topicName;
    private Integer topicCourseAggregateId;
    private Integer topicVersion;
    private AggregateState state;
    @OneToOne(cascade = CascadeType.ALL, mappedBy = "tournamenttopic")
    private Tournament tournament;
    @OneToOne
    private Tournament tournament; 

    public TournamentTopic() {
    }

    public TournamentTopic(TournamentDto tournamentDto) {
        setTopicAggregateId(tournamentDto.getTopicAggregateId());
        setTopicName(tournamentDto.getTopicName());
        setTopicCourseAggregateId(tournamentDto.getTopicCourseAggregateId());
        setTopicVersion(tournamentDto.getTopicVersion());
        setState(tournamentDto.getState());
        setTournament(tournament);
    }

    public TournamentTopic(TournamentTopic other) {
        setTopicAggregateId(other.getTopicAggregateId());
        setTopicName(other.getTopicName());
        setTopicCourseAggregateId(other.getTopicCourseAggregateId());
        setTopicVersion(other.getTopicVersion());
        setState(other.getState());
        setTournament(new Tournament(other.getTournament()));
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

    public Tournament getTournament() {
        return tournament;
    }

    public void setTournament(Tournament tournament) {
        this.tournament = tournament;
        if (this.tournament != null) {
            this.tournament.setTournamentTopic(this);
        }
    }

    public Tournament getTournament() {
        return tournament;
    }

    public void setTournament(Tournament tournament) {
        this.tournament = tournament;
    }


}