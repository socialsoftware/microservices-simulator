package pt.ulisboa.tecnico.socialsoftware.answers.microservices.tournament.aggregate;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.OneToOne;

import pt.ulisboa.tecnico.socialsoftware.answers.shared.dtos.QuizDto;

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

    public TournamentQuiz(QuizDto quizDto) {
        setQuizAggregateId(quizDto.getAggregateId());
        setQuizVersion(quizDto.getVersion());
    }

    public TournamentQuiz(TournamentQuiz other) {
        setQuizVersion(other.getQuizVersion());
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

    public Tournament getTournament() {
        return tournament;
    }

    public void setTournament(Tournament tournament) {
        this.tournament = tournament;
    }


    public QuizDto buildDto() {
        QuizDto dto = new QuizDto();
        dto.setAggregateId(getQuizAggregateId());
        dto.setVersion(getQuizVersion());
        return dto;
    }
}