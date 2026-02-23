package pt.ulisboa.tecnico.socialsoftware.answers.microservices.answer.events.handling.handlers;

import pt.ulisboa.tecnico.socialsoftware.ms.coordination.eventProcessing.EventProcessingHandler;
import pt.ulisboa.tecnico.socialsoftware.ms.domain.aggregate.Aggregate;
import pt.ulisboa.tecnico.socialsoftware.ms.exception.SimulatorException;
import static pt.ulisboa.tecnico.socialsoftware.ms.exception.SimulatorErrorMessage.AGGREGATE_DELETED;
import pt.ulisboa.tecnico.socialsoftware.answers.microservices.answer.aggregate.Answer;
import pt.ulisboa.tecnico.socialsoftware.answers.microservices.answer.repository.AnswerRepository;
import pt.ulisboa.tecnico.socialsoftware.answers.microservices.user.events.publish.UserDeletedEvent;

import org.springframework.stereotype.Component;
import java.util.List;

@Component
public class UserDeletedEventHandler implements EventProcessingHandler<UserDeletedEvent, Answer> {

    private final AnswerRepository answerRepository;

    public UserDeletedEventHandler(AnswerRepository answerRepository) {
        this.answerRepository = answerRepository;
    }

    @Override
    public void handleEvent(Answer answer, UserDeletedEvent event) {
        // Reference constraint: prevent deletion if references exist
        if (answer.getUser() != null) {
            Integer referencedUserId = answer.getUser().getUserAggregateId();
            if (referencedUserId != null && referencedUserId.equals(event.getPublisherAggregateId())) {
                throw new SimulatorException(AGGREGATE_DELETED, "Cannot delete user that has answers");
            }
        }
    }
}
