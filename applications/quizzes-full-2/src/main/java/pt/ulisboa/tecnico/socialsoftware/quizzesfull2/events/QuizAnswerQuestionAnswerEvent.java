package pt.ulisboa.tecnico.socialsoftware.quizzesfull2.events;

import jakarta.persistence.Entity;
import pt.ulisboa.tecnico.socialsoftware.ms.aggregate.Event;

import java.time.LocalDateTime;

@Entity
public class QuizAnswerQuestionAnswerEvent extends Event {
    private Integer quizAnswerAggregateId;
    private Integer questionAggregateId;
    private Integer quizAggregateId;
    private Integer studentAggregateId;
    private Boolean correct;
    private LocalDateTime answerTime;

    protected QuizAnswerQuestionAnswerEvent() {}

    public QuizAnswerQuestionAnswerEvent(Integer quizAnswerAggregateId, Integer questionAggregateId,
                                         Integer quizAggregateId, Integer studentAggregateId, Boolean correct,
                                         LocalDateTime answerTime) {
        super(quizAnswerAggregateId);
        this.quizAnswerAggregateId = quizAnswerAggregateId;
        this.questionAggregateId = questionAggregateId;
        this.quizAggregateId = quizAggregateId;
        this.studentAggregateId = studentAggregateId;
        this.correct = correct;
        this.answerTime = answerTime;
    }

    public Integer getQuizAnswerAggregateId() {
        return quizAnswerAggregateId;
    }

    public Integer getQuestionAggregateId() {
        return questionAggregateId;
    }

    public Integer getQuizAggregateId() {
        return quizAggregateId;
    }

    public Integer getStudentAggregateId() {
        return studentAggregateId;
    }

    public Boolean getCorrect() {
        return correct;
    }

    public LocalDateTime getAnswerTime() {
        return answerTime;
    }
}
