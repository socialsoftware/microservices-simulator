package pt.ulisboa.tecnico.socialsoftware.ms.quizzes.microservices.tournament.aggregate;

import jakarta.persistence.*;
import pt.ulisboa.tecnico.socialsoftware.ms.quizzes.microservices.quiz.aggregate.QuizDto;

@Entity
public class TournamentQuiz {
    @Id
    @GeneratedValue
    private Long id;
    private Integer quizAggregateId;
    private Integer quizVersion;
    @OneToOne
    private Tournament tournament;

    public TournamentQuiz() {

    }
    public TournamentQuiz(Integer quizAggregateId, Integer quizVersion) {
        setQuizAggregateId(quizAggregateId);
        setQuizVersion(quizVersion);
    }

    public TournamentQuiz(TournamentQuiz other) {
        setQuizAggregateId(other.getQuizAggregateId());
        setQuizVersion(other.getQuizVersion());
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getId() {
        return id;
    }

    public Integer getQuizAggregateId() {
        return quizAggregateId;
    }

    public void setQuizAggregateId(Integer id) {
        this.quizAggregateId = id;
    }

    public Integer getQuizVersion() {
        return quizVersion;
    }

    public void setQuizVersion(Integer quizVersion) {
        this.quizVersion = quizVersion;
    }

    public Tournament getTournament() {
        return tournament;
    }

    public void setTournament(Tournament tournament) {
        this.tournament = tournament;
    }

    public QuizDto buildDto() {
        QuizDto quizDto = new QuizDto();
        quizDto.setAggregateId(getQuizAggregateId());
        quizDto.setVersion(quizDto.getVersion());

        return  quizDto;
    }
}
