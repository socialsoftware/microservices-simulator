package pt.ulisboa.tecnico.socialsoftware.blcm.causalconsistency.event;

import pt.ulisboa.tecnico.socialsoftware.blcm.answer.domain.Answer;
import pt.ulisboa.tecnico.socialsoftware.blcm.answer.domain.QuestionAnswer;

import javax.persistence.DiscriminatorValue;
import javax.persistence.Entity;

import static pt.ulisboa.tecnico.socialsoftware.blcm.causalconsistency.event.EventType.ANSWER_QUESTION;

@Entity
@DiscriminatorValue(ANSWER_QUESTION)
public class AnswerQuestionEvent extends DomainEvent {
    private Integer answerAggregateId;

    private Integer questionAggregateId;

    private Integer quizAggregateId;

    private Integer userAggregateId;

    private boolean correct;

    public AnswerQuestionEvent() {
    }

    public AnswerQuestionEvent(QuestionAnswer questionAnswer, Answer answer, Integer quizAggregateId) {
        setQuestionAggregateId(questionAnswer.getQuestionAggregateId());
        setQuizAggregateId(quizAggregateId);
        setCorrect(questionAnswer.isCorrect());
        setUserAggregateId(answer.getUser().getAggregateId());
        setAnswerAggregateId(answer.getAggregateId());
    }

    public Integer getAnswerAggregateId() {
        return answerAggregateId;
    }

    public void setAnswerAggregateId(Integer answerAggregateId) {
        this.answerAggregateId = answerAggregateId;
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

    public boolean isCorrect() {
        return correct;
    }

    public void setCorrect(boolean correct) {
        this.correct = correct;
    }

    public Integer getUserAggregateId() {
        return userAggregateId;
    }

    public void setUserAggregateId(Integer userAggregateId) {
        this.userAggregateId = userAggregateId;
    }
}
