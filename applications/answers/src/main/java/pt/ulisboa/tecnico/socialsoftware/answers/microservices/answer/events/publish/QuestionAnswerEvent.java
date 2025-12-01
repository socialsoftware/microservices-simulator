package pt.ulisboa.tecnico.socialsoftware.answers.microservices.answer.events.publish;

import jakarta.persistence.Entity;
import pt.ulisboa.tecnico.socialsoftware.ms.domain.event.Event;

@Entity
public class QuestionAnswerEvent extends Event {
    private Integer questionAggregateId;
    private Integer quizAggregateId;
    private Integer userAggregateId;
    private boolean correct;

    public QuestionAnswerEvent() {
    }

    public QuestionAnswerEvent(Integer aggregateId, Integer questionAggregateId, Integer quizAggregateId, Integer userAggregateId, boolean correct) {
        super(aggregateId);
        setQuestionAggregateId(questionAggregateId);
        setQuizAggregateId(quizAggregateId);
        setUserAggregateId(userAggregateId);
        setCorrect(correct);
    }

    public Integer getQuestionAggregateId() {
        return questionAggregateId;
    }

    public void setQuestionAggregateId(Integer questionAggregateId) {
        this.questionAggregateId = questionAggregateId;
    }

    public Integer getQuizAggregateId() {
        return quizAggregateId;
    }

    public void setQuizAggregateId(Integer quizAggregateId) {
        this.quizAggregateId = quizAggregateId;
    }

    public Integer getUserAggregateId() {
        return userAggregateId;
    }

    public void setUserAggregateId(Integer userAggregateId) {
        this.userAggregateId = userAggregateId;
    }

    public boolean getCorrect() {
        return correct;
    }

    public void setCorrect(boolean correct) {
        this.correct = correct;
    }

}