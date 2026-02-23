package pt.ulisboa.tecnico.socialsoftware.answers.microservices.question.events.handling.handlers;

import pt.ulisboa.tecnico.socialsoftware.ms.coordination.eventProcessing.EventProcessingHandler;
import pt.ulisboa.tecnico.socialsoftware.ms.domain.aggregate.Aggregate;
import pt.ulisboa.tecnico.socialsoftware.ms.exception.SimulatorException;
import static pt.ulisboa.tecnico.socialsoftware.ms.exception.SimulatorErrorMessage.AGGREGATE_DELETED;
import pt.ulisboa.tecnico.socialsoftware.answers.microservices.question.aggregate.Question;
import pt.ulisboa.tecnico.socialsoftware.answers.microservices.question.repository.QuestionRepository;
import pt.ulisboa.tecnico.socialsoftware.answers.microservices.course.events.publish.CourseDeletedEvent;

import org.springframework.stereotype.Component;
import java.util.List;

@Component
public class CourseDeletedEventHandler implements EventProcessingHandler<CourseDeletedEvent, Question> {

    private final QuestionRepository questionRepository;

    public CourseDeletedEventHandler(QuestionRepository questionRepository) {
        this.questionRepository = questionRepository;
    }

    @Override
    public void handleEvent(Question question, CourseDeletedEvent event) {
        // Reference constraint: prevent deletion if references exist
        if (question.getCourse() != null) {
            Integer referencedCourseId = question.getCourse().getCourseAggregateId();
            if (referencedCourseId != null && referencedCourseId.equals(event.getPublisherAggregateId())) {
                throw new SimulatorException(AGGREGATE_DELETED, "Cannot delete course that has questions");
            }
        }
    }
}
