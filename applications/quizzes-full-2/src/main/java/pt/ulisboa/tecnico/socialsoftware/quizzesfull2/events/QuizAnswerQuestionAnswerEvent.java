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

    // Anchored on the quiz, not on the publishing quiz answer. The only consumer is Tournament, which
    // caches the quiz it generated but never learns a quiz answer id - no functionality links the two -
    // so a subscription anchored on quizAnswerAggregateId could never be constructed. The consumer
    // discriminates on studentAggregateId in its service ByEvent method and links quizAnswerAggregateId
    // from this payload on the first answer.
    public QuizAnswerQuestionAnswerEvent(Integer quizAnswerAggregateId, Integer questionAggregateId,
                                         Integer quizAggregateId, Integer studentAggregateId, Boolean correct,
                                         LocalDateTime answerTime) {
        super(quizAggregateId);
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
