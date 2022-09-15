package pt.ulisboa.tecnico.socialsoftware.blcm.causalconsistency.event;

import javax.persistence.DiscriminatorValue;
import javax.persistence.Entity;

@Entity
@DiscriminatorValue(EventType.REMOVE_COURSE_EXECUTION)
public class RemoveCourseExecutionEvent extends DomainEvent {
    private Integer courseExecutionAggregateId;

    public RemoveCourseExecutionEvent() {

    }

    public RemoveCourseExecutionEvent(Integer courseExecutionAggregateId) {
        this.courseExecutionAggregateId = courseExecutionAggregateId;
    }



    public Integer getCourseExecutionId() {
        return courseExecutionAggregateId;
    }

    public void setCourseExecutionAggregateId(Integer coureseExecutionId) {
        this.courseExecutionAggregateId = coureseExecutionId;
    }
}