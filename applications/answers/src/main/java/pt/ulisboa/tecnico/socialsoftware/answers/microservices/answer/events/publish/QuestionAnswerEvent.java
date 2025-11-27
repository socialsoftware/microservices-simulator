package pt.ulisboa.tecnico.socialsoftware.answers.microservices.answer.events.publish;

import jakarta.persistence.Entity;
import pt.ulisboa.tecnico.socialsoftware.ms.domain.event.Event;

@Entity
public class QuestionAnswerEvent extends Event {
    private Integer questionAggregateId;
    private Integer quizAggregateId;
    private Integer studentAggregateId;
    private Boolean correct;

    public QuestionAnswerEvent() {
    }

    public QuestionAnswerEvent(Integer aggregateId, Integer questionAggregateId, Integer quizAggregateId, Integer studentAggregateId, Boolean correct) {
        super(aggregateId);
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

    public Integer getStudentAggregateId() {
        return studentAggregateId;
    }

    public void setStudentAggregateId(Integer studentAggregateId) {
        this.studentAggregateId = studentAggregateId;
    }

    public Boolean getCorrect() {
        return correct;
    }

    public void setCorrect(Boolean correct) {
        this.correct = correct;
    }

}