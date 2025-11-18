package pt.ulisboa.tecnico.socialsoftware.answers.microservices.tournament.aggregate;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.OneToOne;

import pt.ulisboa.tecnico.socialsoftware.answers.microservices.quiz.aggregate.QuizDto;

import pt.ulisboa.tecnico.socialsoftware.answers.shared.enums.QuizType;

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
        setTitle(quizDto.getTitle());
        setQuizType(QuizType.valueOf(quizDto.getQuizType()));
        setCreationDate(quizDto.getCreationDate());
        setAvailableDate(quizDto.getAvailableDate());
        setConclusionDate(quizDto.getConclusionDate());
        setResultsDate(quizDto.getResultsDate());
        setNumberOfQuestions(quizDto.getNumberOfQuestions());
        setQuestions(quizDto.getQuestions());
    }

    public TournamentQuiz(TournamentQuiz other) {
        setQuizAggregateId(other.getQuizAggregateId());
        setQuizVersion(other.getQuizVersion());
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

    public Tournament getTournament() {
        return tournament;
    }

    public void setTournament(Tournament tournament) {
        this.tournament = tournament;
    }


    public QuizDto buildDto() {
        QuizDto dto = new QuizDto();
        dto.setAggregateId(getAggregateId());
        dto.setVersion(getVersion());
        dto.setState(getState());
        return dto;
    }
}