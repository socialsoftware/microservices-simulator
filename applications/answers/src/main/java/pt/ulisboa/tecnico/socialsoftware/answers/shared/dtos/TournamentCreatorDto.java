package pt.ulisboa.tecnico.socialsoftware.answers.shared.dtos;

import java.io.Serializable;
import pt.ulisboa.tecnico.socialsoftware.ms.domain.aggregate.Aggregate.AggregateState;
import pt.ulisboa.tecnico.socialsoftware.answers.microservices.tournament.aggregate.TournamentCreator;

public class TournamentCreatorDto implements Serializable {
    private Integer aggregateId;
    private Integer version;
    private AggregateState state;

    public TournamentCreatorDto() {
    }

    public TournamentCreatorDto(TournamentCreator tournamentCreator) {
        this.aggregateId = tournamentCreator.getCreatorAggregateId();
        this.version = tournamentCreator.getCreatorVersion();
        this.state = tournamentCreator.getCreatorState();
    }

    public Integer getAggregateId() {
        return aggregateId;
    }

    public void setAggregateId(Integer aggregateId) {
        this.aggregateId = aggregateId;
    }

    public Integer getVersion() {
        return version;
    }

    public void setVersion(Integer version) {
        this.version = version;
    }

    public AggregateState getState() {
        return state;
    }

    public void setState(AggregateState state) {
        this.state = state;
    }
}