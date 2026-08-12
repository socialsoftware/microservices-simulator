package pt.ulisboa.tecnico.socialsoftware.answers.shared.dtos;

import java.io.Serializable;
import pt.ulisboa.tecnico.socialsoftware.answers.microservices.tournament.aggregate.TournamentCreator;

public class TournamentCreatorDto implements Serializable {
    private Integer aggregateId;
    private Integer version;
    private String state;

    public TournamentCreatorDto() {
    }

    public TournamentCreatorDto(TournamentCreator tournamentCreator) {
        this.aggregateId = tournamentCreator.getCreatorAggregateId();
        this.version = tournamentCreator.getCreatorVersion();
        this.state = tournamentCreator.getCreatorState() != null ? tournamentCreator.getCreatorState().name() : null;
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

    public String getState() {
        return state;
    }

    public void setState(String state) {
        this.state = state;
    }
}