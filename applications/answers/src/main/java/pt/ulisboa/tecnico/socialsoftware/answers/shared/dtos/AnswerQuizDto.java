package pt.ulisboa.tecnico.socialsoftware.answers.shared.dtos;

import java.io.Serializable;
import java.util.Set;
import pt.ulisboa.tecnico.socialsoftware.answers.microservices.answer.aggregate.AnswerQuiz;

public class AnswerQuizDto implements Serializable {
    private Integer aggregateId;
    private Integer version;
    private Set<Integer> quizQuestionsAggregateIds;
    private String state;

    public AnswerQuizDto() {
    }

    public AnswerQuizDto(AnswerQuiz answerQuiz) {
        this.aggregateId = answerQuiz.getQuizAggregateId();
        this.version = answerQuiz.getQuizVersion();
        this.quizQuestionsAggregateIds = answerQuiz.getQuizQuestionsAggregateIds();
        this.state = answerQuiz.getQuizState() != null ? answerQuiz.getQuizState().name() : null;
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

    public Set<Integer> getQuizQuestionsAggregateIds() {
        return quizQuestionsAggregateIds;
    }

    public void setQuizQuestionsAggregateIds(Set<Integer> quizQuestionsAggregateIds) {
        this.quizQuestionsAggregateIds = quizQuestionsAggregateIds;
    }

    public String getState() {
        return state;
    }

    public void setState(String state) {
        this.state = state;
    }
}