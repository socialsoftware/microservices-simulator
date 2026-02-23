package pt.ulisboa.tecnico.socialsoftware.answers.microservices.answer.events.handling.handlers;

import pt.ulisboa.tecnico.socialsoftware.ms.coordination.eventProcessing.EventProcessingHandler;
import pt.ulisboa.tecnico.socialsoftware.ms.domain.aggregate.Aggregate;
import pt.ulisboa.tecnico.socialsoftware.ms.exception.SimulatorException;
import static pt.ulisboa.tecnico.socialsoftware.ms.exception.SimulatorErrorMessage.AGGREGATE_DELETED;
import pt.ulisboa.tecnico.socialsoftware.answers.microservices.answer.aggregate.Answer;
import pt.ulisboa.tecnico.socialsoftware.answers.microservices.answer.repository.AnswerRepository;
import pt.ulisboa.tecnico.socialsoftware.answers.microservices.execution.events.publish.ExecutionDeletedEvent;

import org.springframework.stereotype.Component;
import java.util.List;

@Component
public class ExecutionDeletedEventHandler implements EventProcessingHandler<ExecutionDeletedEvent, Answer> {

    private final AnswerRepository answerRepository;

    public ExecutionDeletedEventHandler(AnswerRepository answerRepository) {
        this.answerRepository = answerRepository;
    }

    @Override
    public void handleEvent(Answer answer, ExecutionDeletedEvent event) {
        // Reference constraint: prevent deletion if references exist
        if (answer.getExecution() != null) {
            Integer referencedExecutionId = answer.getExecution().getExecutionAggregateId();
            if (referencedExecutionId != null && referencedExecutionId.equals(event.getPublisherAggregateId())) {
                throw new SimulatorException(AGGREGATE_DELETED, "Cannot delete execution that has answers");
            }
        }
    }
}
