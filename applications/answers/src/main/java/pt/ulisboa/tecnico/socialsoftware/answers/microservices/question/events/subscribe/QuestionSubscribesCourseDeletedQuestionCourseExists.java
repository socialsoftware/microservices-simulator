package pt.ulisboa.tecnico.socialsoftware.answers.microservices.question.events.subscribe;

import pt.ulisboa.tecnico.socialsoftware.ms.domain.event.Event;
import pt.ulisboa.tecnico.socialsoftware.ms.domain.event.EventSubscription;
import pt.ulisboa.tecnico.socialsoftware.answers.microservices.question.aggregate.QuestionCourse;
import pt.ulisboa.tecnico.socialsoftware.answers.microservices.course.events.publish.CourseDeletedEvent;


public class QuestionSubscribesCourseDeletedQuestionCourseExists extends EventSubscription {
    public QuestionSubscribesCourseDeletedQuestionCourseExists(QuestionCourse course) {
        super(course.getCourseAggregateId(),
                course.getCourseVersion(),
                CourseDeletedEvent.class);
    }

    @Override
    public boolean subscribesEvent(Event event) {
         return super.subscribesEvent(event);
    }

}
