package pt.ulisboa.tecnico.socialsoftware.answers.microservices.tournament.aggregate;

import jakarta.persistence.Embeddable;
import pt.ulisboa.tecnico.socialsoftware.ms.coordination.unitOfWork.UnitOfWork;
import pt.ulisboa.tecnico.socialsoftware.ms.domain.aggregate.Aggregate.AggregateState;

@Embeddable
public class TournamentQuiz {
    private Long id;
    private Integer quizAggregateId;
    private Integer quizVersion;
    private Object tournament; 

    public TournamentQuiz(Long id, Integer quizAggregateId, Integer quizVersion, Object tournament) {
        this.id = id;
        this.quizAggregateId = quizAggregateId;
        this.quizVersion = quizVersion;
        this.tournament = tournament;
    }

    public TournamentQuiz(TournamentQuiz other) {
        // Copy constructor
    }


    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Integer getQuizAggregateId() {
        return quizAggregateId;
    }

    public void setQuizAggregateId(Integer quizAggregateId) {
        this.quizAggregateId = quizAggregateId;
    }

    public Integer getQuizVersion() {
        return quizVersion;
    }

    public void setQuizVersion(Integer quizVersion) {
        this.quizVersion = quizVersion;
    }

    public Object getTournament() {
        return tournament;
    }

    public void setTournament(Object tournament) {
        this.tournament = tournament;
    }


}