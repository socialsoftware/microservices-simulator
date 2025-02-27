package pt.ulisboa.tecnico.socialsoftware.ms.quizzes.microservices.answer.events.publish;

import jakarta.persistence.Entity;
import pt.ulisboa.tecnico.socialsoftware.ms.domain.event.Event;

@Entity
public class QuizAnswerQuestionAnswerEvent extends Event {
    private Integer questionAggregateId;
    private Integer quizAggregateId;
    private Integer studentAggregateId;
    private boolean correct;

    public QuizAnswerQuestionAnswerEvent() {}

    public QuizAnswerQuestionAnswerEvent(Integer quizAnswerAggregateId, Integer questionAggregateId, Integer quizAggregateId, Integer studentAggregateId, boolean correct) {
        super(quizAnswerAggregateId);
        setQuestionAggregateId(questionAggregateId);
        setQuizAggregateId(quizAggregateId);
        setStudentAggregateId(studentAggregateId);
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

    public boolean isCorrect() {
        return correct;
    }

    public void setCorrect(boolean correct) {
        this.correct = correct;
    }

    public Integer getStudentAggregateId() {
        return studentAggregateId;
    }

    public void setStudentAggregateId(Integer studentAggregateId) {
        this.studentAggregateId = studentAggregateId;
    }
}
