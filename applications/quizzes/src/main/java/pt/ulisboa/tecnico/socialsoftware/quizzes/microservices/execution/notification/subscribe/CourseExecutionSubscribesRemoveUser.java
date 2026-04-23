package pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.execution.notification.subscribe;

import pt.ulisboa.tecnico.socialsoftware.ms.aggregate.Event;
import pt.ulisboa.tecnico.socialsoftware.ms.aggregate.EventSubscription;
import pt.ulisboa.tecnico.socialsoftware.quizzes.events.DeleteUserEvent;
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.execution.aggregate.CourseExecutionStudent;

public class CourseExecutionSubscribesRemoveUser extends EventSubscription {
    private CourseExecutionStudent courseExecutionStudent;

    public CourseExecutionSubscribesRemoveUser(CourseExecutionStudent courseExecutionStudent) {
        super(courseExecutionStudent.getUserAggregateId(),
                courseExecutionStudent.getUserVersion(),
                DeleteUserEvent.class.getSimpleName());
        this.courseExecutionStudent = courseExecutionStudent;
    }

    public CourseExecutionSubscribesRemoveUser() {}

    public boolean subscribesEvent(Event event) {
         return super.subscribesEvent(event) && checkCourseExecutionInfo((DeleteUserEvent)event);
    }

    private boolean checkCourseExecutionInfo(DeleteUserEvent event) {
        return this.courseExecutionStudent.getUserAggregateId().equals(event.getPublisherAggregateId()) && this.courseExecutionStudent.isActive();
    }

}