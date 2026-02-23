package pt.ulisboa.tecnico.socialsoftware.answers.microservices.answer.events.handling.handlers;

import pt.ulisboa.tecnico.socialsoftware.ms.coordination.eventProcessing.EventProcessingHandler;
import pt.ulisboa.tecnico.socialsoftware.ms.domain.aggregate.Aggregate;
import pt.ulisboa.tecnico.socialsoftware.ms.exception.SimulatorException;
import static pt.ulisboa.tecnico.socialsoftware.ms.exception.SimulatorErrorMessage.AGGREGATE_DELETED;
import pt.ulisboa.tecnico.socialsoftware.answers.microservices.answer.aggregate.Answer;
import pt.ulisboa.tecnico.socialsoftware.answers.microservices.answer.repository.AnswerRepository;
import pt.ulisboa.tecnico.socialsoftware.answers.microservices.quiz.events.publish.QuizDeletedEvent;

import org.springframework.stereotype.Component;
import java.util.List;

@Component
public class QuizDeletedEventHandler implements EventProcessingHandler<QuizDeletedEvent, Answer> {

    private final AnswerRepository answerRepository;

    public QuizDeletedEventHandler(AnswerRepository answerRepository) {
        this.answerRepository = answerRepository;
    }

    @Override
    public void handleEvent(Answer answer, QuizDeletedEvent event) {
        // Reference constraint: prevent deletion if references exist
        if (answer.getQuiz() != null) {
            Integer referencedQuizId = answer.getQuiz().getQuizAggregateId();
            if (referencedQuizId != null && referencedQuizId.equals(event.getPublisherAggregateId())) {
                throw new SimulatorException(AGGREGATE_DELETED, "Cannot delete quiz that has answers");
            }
        }
    }
}
