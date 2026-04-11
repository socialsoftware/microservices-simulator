package pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.execution.notification.handling.handlers;

import pt.ulisboa.tecnico.socialsoftware.ms.notification.Event;
import pt.ulisboa.tecnico.socialsoftware.ms.notification.EventHandler;
import pt.ulisboa.tecnico.socialsoftware.ms.notification.EventSubscription;
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.execution.aggregate.CourseExecutionRepository;
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.execution.coordination.eventProcessing.ExecutionEventProcessing;

import java.util.HashSet;
import java.util.Set;

public abstract class CourseExecutionEventHandler extends EventHandler {
    private final CourseExecutionRepository courseExecutionRepository;
    protected ExecutionEventProcessing executionEventProcessing;

    public CourseExecutionEventHandler(CourseExecutionRepository courseExecutionRepository, ExecutionEventProcessing executionEventProcessing) {
        this.courseExecutionRepository = courseExecutionRepository;
        this.executionEventProcessing = executionEventProcessing;
    }

    @Override
    public Set<Integer> getAggregateIds() {
        return courseExecutionRepository.findAllAggregateIds();
    }

    @Override
    public Set<EventSubscription> getEventSubscriptions(Integer subscriberAggregateId, Class<? extends Event> eventClass) {
        return courseExecutionRepository.findLastAggregateVersion(subscriberAggregateId)
                .map(aggregate -> aggregate.getEventSubscriptionsByEventType(eventClass.getSimpleName()))
                .orElse(new HashSet<>());
    }

}
