package pt.ulisboa.tecnico.socialsoftware.answers.microservices.topic.events.handling.handlers;

import pt.ulisboa.tecnico.socialsoftware.ms.coordination.eventProcessing.EventProcessingHandler;
import pt.ulisboa.tecnico.socialsoftware.ms.domain.aggregate.Aggregate;
import pt.ulisboa.tecnico.socialsoftware.ms.exception.SimulatorException;
import static pt.ulisboa.tecnico.socialsoftware.ms.exception.SimulatorErrorMessage.AGGREGATE_DELETED;
import pt.ulisboa.tecnico.socialsoftware.answers.microservices.topic.aggregate.Topic;
import pt.ulisboa.tecnico.socialsoftware.answers.microservices.topic.repository.TopicRepository;
import pt.ulisboa.tecnico.socialsoftware.answers.microservices.course.events.publish.CourseDeletedEvent;

import org.springframework.stereotype.Component;
import java.util.List;

@Component
public class CourseDeletedEventHandler implements EventProcessingHandler<CourseDeletedEvent, Topic> {

    private final TopicRepository topicRepository;

    public CourseDeletedEventHandler(TopicRepository topicRepository) {
        this.topicRepository = topicRepository;
    }

    @Override
    public void handleEvent(Topic topic, CourseDeletedEvent event) {
        // Reference constraint: prevent deletion if references exist
        if (topic.getCourse() != null) {
            Integer referencedCourseId = topic.getCourse().getCourseAggregateId();
            if (referencedCourseId != null && referencedCourseId.equals(event.getPublisherAggregateId())) {
                throw new SimulatorException(AGGREGATE_DELETED, "Cannot delete course that has topics");
            }
        }
    }
}
