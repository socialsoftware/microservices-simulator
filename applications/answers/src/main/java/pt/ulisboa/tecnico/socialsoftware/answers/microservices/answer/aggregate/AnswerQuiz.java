package pt.ulisboa.tecnico.socialsoftware.answers.microservices.answer.aggregate;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.OneToOne;

import pt.ulisboa.tecnico.socialsoftware.ms.domain.aggregate.Aggregate.AggregateState;

import pt.ulisboa.tecnico.socialsoftware.answers.shared.dtos.AnswerQuizDto;
import pt.ulisboa.tecnico.socialsoftware.answers.shared.dtos.QuizDto;

@Entity
public class AnswerQuiz {
    @Id
    @GeneratedValue
    private Long id;
    private Integer quizAggregateId;
    private Integer quizVersion;
    private AggregateState quizState;
    @OneToOne
    private Answer answer;

    public AnswerQuiz() {

    }

    public AnswerQuiz(QuizDto quizDto) {
        setQuizAggregateId(quizDto.getAggregateId());
        setQuizVersion(quizDto.getVersion());
        setQuizState(quizDto.getState());
    }

    public AnswerQuiz(AnswerQuizDto answerQuizDto) {
        setQuizAggregateId(answerQuizDto.getAggregateId());
        setQuizVersion(answerQuizDto.getVersion());
        setQuizState(answerQuizDto.getState());
    }

    public AnswerQuiz(AnswerQuiz other) {
        setQuizAggregateId(other.getQuizAggregateId());
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

    public Answer getAnswer() {
        return answer;
    }

    public void setAnswer(Answer answer) {
        this.answer = answer;
    }




    public AnswerQuizDto buildDto() {
        AnswerQuizDto dto = new AnswerQuizDto();
        dto.setAggregateId(getQuizAggregateId());
        dto.setVersion(getQuizVersion());
        dto.setState(getQuizState());
        return dto;
    }
}