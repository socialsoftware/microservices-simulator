package pt.ulisboa.tecnico.socialsoftware.quizzes.events;

import jakarta.persistence.Entity;
import pt.ulisboa.tecnico.socialsoftware.ms.domain.event.Event;

/**
 * Emitted when a student starts a quiz (a new QuizAnswer is created).
 * publisherAggregateId = quizAggregateId, so the Quiz aggregate can subscribe
 * to its own answer-creation events for TCC causal consistency tracking.
 */
@Entity
public class CreateQuizAnswerEvent extends Event {
    private Integer quizAggregateId;
    private Integer studentAggregateId;

    public CreateQuizAnswerEvent() {
        super();
    }

    public CreateQuizAnswerEvent(Integer quizAggregateId, Integer studentAggregateId) {
        super(quizAggregateId);
        this.quizAggregateId = quizAggregateId;
        this.studentAggregateId = studentAggregateId;
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
}
