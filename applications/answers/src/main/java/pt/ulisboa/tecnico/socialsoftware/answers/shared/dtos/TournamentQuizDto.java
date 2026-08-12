package pt.ulisboa.tecnico.socialsoftware.answers.shared.dtos;

import java.io.Serializable;
import pt.ulisboa.tecnico.socialsoftware.answers.microservices.tournament.aggregate.TournamentQuiz;

public class TournamentQuizDto implements Serializable {
    private Integer aggregateId;
    private Integer version;
    private String state;

    public TournamentQuizDto() {
    }

    public TournamentQuizDto(TournamentQuiz tournamentQuiz) {
        this.aggregateId = tournamentQuiz.getQuizAggregateId();
        this.version = tournamentQuiz.getQuizVersion();
        this.state = tournamentQuiz.getQuizState() != null ? tournamentQuiz.getQuizState().name() : null;
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