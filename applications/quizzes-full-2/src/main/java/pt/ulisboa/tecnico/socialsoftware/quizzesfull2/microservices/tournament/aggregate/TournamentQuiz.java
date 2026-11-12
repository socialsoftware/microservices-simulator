package pt.ulisboa.tecnico.socialsoftware.quizzesfull2.microservices.tournament.aggregate;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "tournament_quizzes")
public class TournamentQuiz {
    @Id
    @GeneratedValue
    private Integer id;
    private Integer quizAggregateId;
    private Long quizVersion;
    @OneToOne
    private Tournament tournament;

    public TournamentQuiz() {
    }

    public TournamentQuiz(Integer quizAggregateId, Long quizVersion) {
        this.quizAggregateId = quizAggregateId;
        this.quizVersion = quizVersion;
    }

    public TournamentQuiz(TournamentQuiz other) {
        this.quizAggregateId = other.getQuizAggregateId();
        this.quizVersion = other.getQuizVersion();
    }

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public Integer getQuizAggregateId() {
        return quizAggregateId;
    }

    public void setQuizAggregateId(Integer quizAggregateId) {
        this.quizAggregateId = quizAggregateId;
    }

    public Long getQuizVersion() {
        return quizVersion;
    }

    public void setQuizVersion(Long quizVersion) {
        this.quizVersion = quizVersion;
    }

    public Tournament getTournament() {
        return tournament;
    }

    public void setTournament(Tournament tournament) {
        this.tournament = tournament;
    }
}
