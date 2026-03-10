package pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.execution.events.subscribe;

import pt.ulisboa.tecnico.socialsoftware.ms.domain.event.Event;
import pt.ulisboa.tecnico.socialsoftware.ms.domain.event.EventSubscription;
import pt.ulisboa.tecnico.socialsoftware.quizzes.events.CreateQuestionEvent;
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.execution.aggregate.CourseExecutionCourse;

public class ExecutionSubscribesCreateQuestion extends EventSubscription {
    private Integer courseAggregateId;

    public ExecutionSubscribesCreateQuestion(CourseExecutionCourse course) {
        super(course.getCourseAggregateId(), course.getCourseVersion(), CreateQuestionEvent.class.getSimpleName());
        this.courseAggregateId = course.getCourseAggregateId();
    }

    public ExecutionSubscribesCreateQuestion() {
    }

    @Override
    public boolean subscribesEvent(Event event) {
        if (!(event instanceof CreateQuestionEvent))
            return false;
        return ((CreateQuestionEvent) event).getCourseAggregateId().equals(this.courseAggregateId);
    }
}
