package pt.ulisboa.tecnico.socialsoftware.quizzesfull.events;

import jakarta.persistence.Entity;
import pt.ulisboa.tecnico.socialsoftware.ms.aggregate.Event;

@Entity
public class QuizAnswerQuestionAnswerEvent extends Event {

    private Integer quizAggregateId;
    private Integer userAggregateId;

    public QuizAnswerQuestionAnswerEvent() {
        super();
    }

    public QuizAnswerQuestionAnswerEvent(Integer quizAnswerAggregateId, Integer quizAggregateId, Integer userAggregateId) {
        super(quizAnswerAggregateId);
        this.quizAggregateId = quizAggregateId;
        this.userAggregateId = userAggregateId;
    }

    public Integer getQuizAggregateId() { return quizAggregateId; }
    public Integer getUserAggregateId() { return userAggregateId; }
}
