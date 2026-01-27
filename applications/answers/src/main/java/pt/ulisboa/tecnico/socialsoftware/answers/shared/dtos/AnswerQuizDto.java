package pt.ulisboa.tecnico.socialsoftware.answers.shared.dtos;

import java.io.Serializable;
import java.util.List;
import pt.ulisboa.tecnico.socialsoftware.ms.domain.aggregate.Aggregate.AggregateState;
import pt.ulisboa.tecnico.socialsoftware.answers.microservices.answer.aggregate.AnswerQuiz;

public class AnswerQuizDto implements Serializable {
    private Integer aggregateId;
    private Integer version;
    private List<Integer> questions;
    private AggregateState state;

    public AnswerQuizDto() {
    }

    public AnswerQuizDto(AnswerQuiz answerQuiz) {
        this.aggregateId = answerQuiz.getQuizAggregateId();
        this.version = answerQuiz.getQuizVersion();
        this.questions = answerQuiz.getQuizQuestionsAggregateIds();
        this.state = answerQuiz.getQuizState();
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

    public List<Integer> getQuestions() {
        return questions;
    }

    public void setQuestions(List<Integer> questions) {
        this.questions = questions;
    }

    public AggregateState getState() {
        return state;
    }

    public void setState(AggregateState state) {
        this.state = state;
    }
}