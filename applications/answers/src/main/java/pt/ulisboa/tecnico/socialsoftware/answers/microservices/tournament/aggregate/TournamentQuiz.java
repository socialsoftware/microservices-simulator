package pt.ulisboa.tecnico.socialsoftware.answers.microservices.tournament.aggregate;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.OneToOne;

import pt.ulisboa.tecnico.socialsoftware.ms.domain.aggregate.Aggregate.AggregateState;

import pt.ulisboa.tecnico.socialsoftware.answers.shared.dtos.QuizDto;
import pt.ulisboa.tecnico.socialsoftware.answers.shared.dtos.TournamentQuizDto;

@Entity
public class TournamentQuiz {
    @Id
    @GeneratedValue
    private Long id;
    private Integer quizAggregateId;
    private Integer quizVersion;
    private AggregateState quizState;
    @OneToOne
    private Tournament tournament;

    public TournamentQuiz() {

    }

    public TournamentQuiz(QuizDto quizDto) {
        setQuizAggregateId(quizDto.getAggregateId());
        setQuizVersion(quizDto.getVersion());
        setQuizState(quizDto.getState());
    }

    public TournamentQuiz(TournamentQuiz other) {
        setQuizVersion(other.getQuizVersion());
        setQuizState(other.getQuizState());
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

    public AggregateState getQuizState() {
        return quizState;
    }

    public void setQuizState(AggregateState quizState) {
        this.quizState = quizState;
    }

    public Tournament getTournament() {
        return tournament;
    }

    public void setTournament(Tournament tournament) {
        this.tournament = tournament;
    }


    public TournamentQuizDto buildDto() {
        TournamentQuizDto dto = new TournamentQuizDto();
        dto.setAggregateId(getQuizAggregateId());
        dto.setVersion(getQuizVersion());
        dto.setState(getQuizState());
        return dto;
    }
}