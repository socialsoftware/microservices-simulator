package pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.execution.events.subscribe;

import pt.ulisboa.tecnico.socialsoftware.ms.domain.event.Event;
import pt.ulisboa.tecnico.socialsoftware.ms.domain.event.EventSubscription;
import pt.ulisboa.tecnico.socialsoftware.quizzes.events.DeleteQuestionEvent;
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.execution.aggregate.CourseExecutionCourse;

public class ExecutionSubscribesDeleteQuestion extends EventSubscription {
    private Integer courseAggregateId;

    public ExecutionSubscribesDeleteQuestion(CourseExecutionCourse course) {
        super(course.getCourseAggregateId(), course.getCourseVersion(), DeleteQuestionEvent.class.getSimpleName());
        this.courseAggregateId = course.getCourseAggregateId();
    }

    public ExecutionSubscribesDeleteQuestion() {
    }

    @Override
    public boolean subscribesEvent(Event event) {
        if (!(event instanceof DeleteQuestionEvent))
            return false;
        return ((DeleteQuestionEvent) event).getCourseAggregateId().equals(this.courseAggregateId);
    }
}
