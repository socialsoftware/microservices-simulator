package pt.ulisboa.tecnico.socialsoftware.answers.microservices.question.events.subscribe;

import pt.ulisboa.tecnico.socialsoftware.ms.domain.event.EventSubscription;
import pt.ulisboa.tecnico.socialsoftware.answers.microservices.question.aggregate.Question;
import pt.ulisboa.tecnico.socialsoftware.answers.microservices.course.events.publish.CourseDeletedEvent;

public class QuestionSubscribesCourseDeleted extends EventSubscription {
    public QuestionSubscribesCourseDeleted(Question question) {
        super(question.getAggregateId(), 0, CourseDeletedEvent.class);
    }
}
