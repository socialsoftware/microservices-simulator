package pt.ulisboa.tecnico.socialsoftware.quizzes.events;

import jakarta.persistence.Entity;
import pt.ulisboa.tecnico.socialsoftware.ms.aggregate.Event;

/**
 * Note: we pass `courseAggregateId` to super() so `publisherAggregateId` is the course id.
 * `publisherAggregateId` identifies the event stream that other aggregates subscribe to,
 * and `CausalUnitOfWork` detects causal conflicts by comparing
 * event.getPublisherAggregateId() == subscription.getSubscribedAggregateId().
 * Using the question id here would prevent those subscription-based causal checks
 * from matching and could silently bypass TCC consistency checks.
 */
@Entity
public class CreateQuestionEvent extends Event {
    private Integer courseAggregateId;

    public CreateQuestionEvent() {
        super();
    }

    public CreateQuestionEvent(Integer courseAggregateId) {
        super(courseAggregateId);
        this.courseAggregateId = courseAggregateId;
    }

    public Integer getCourseAggregateId() {
        return courseAggregateId;
    }

    public void setCourseAggregateId(Integer courseAggregateId) {
        this.courseAggregateId = courseAggregateId;
    }
}
