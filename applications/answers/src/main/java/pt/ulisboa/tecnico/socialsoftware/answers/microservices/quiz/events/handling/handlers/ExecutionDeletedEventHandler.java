package pt.ulisboa.tecnico.socialsoftware.answers.microservices.quiz.events.handling.handlers;

import pt.ulisboa.tecnico.socialsoftware.ms.coordination.eventProcessing.EventProcessingHandler;
import pt.ulisboa.tecnico.socialsoftware.ms.domain.aggregate.Aggregate;
import pt.ulisboa.tecnico.socialsoftware.ms.exception.SimulatorException;
import static pt.ulisboa.tecnico.socialsoftware.ms.exception.SimulatorErrorMessage.AGGREGATE_DELETED;
import pt.ulisboa.tecnico.socialsoftware.answers.microservices.quiz.aggregate.Quiz;
import pt.ulisboa.tecnico.socialsoftware.answers.microservices.quiz.repository.QuizRepository;
import pt.ulisboa.tecnico.socialsoftware.answers.microservices.execution.events.publish.ExecutionDeletedEvent;

import org.springframework.stereotype.Component;
import java.util.List;

@Component
public class ExecutionDeletedEventHandler implements EventProcessingHandler<ExecutionDeletedEvent, Quiz> {

    private final QuizRepository quizRepository;

    public ExecutionDeletedEventHandler(QuizRepository quizRepository) {
        this.quizRepository = quizRepository;
    }

    @Override
    public void handleEvent(Quiz quiz, ExecutionDeletedEvent event) {
        // Reference constraint: prevent deletion if references exist
        if (quiz.getExecution() != null) {
            Integer referencedExecutionId = quiz.getExecution().getExecutionAggregateId();
            if (referencedExecutionId != null && referencedExecutionId.equals(event.getPublisherAggregateId())) {
                throw new SimulatorException(AGGREGATE_DELETED, "Cannot delete execution that has quizzes");
            }
        }
    }
}
