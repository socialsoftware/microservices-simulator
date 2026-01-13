package pt.ulisboa.tecnico.socialsoftware.answers.shared.dtos;

import java.io.Serializable;
import pt.ulisboa.tecnico.socialsoftware.answers.microservices.tournament.aggregate.TournamentQuiz;

public class TournamentQuizDto implements Serializable {
    private Integer aggregateId;
    private Integer version;

    public TournamentQuizDto() {
    }

    public TournamentQuizDto(TournamentQuiz tournamentQuiz) {
        this.aggregateId = tournamentQuiz.getQuizAggregateId();
        this.version = tournamentQuiz.getQuizVersion();
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
}