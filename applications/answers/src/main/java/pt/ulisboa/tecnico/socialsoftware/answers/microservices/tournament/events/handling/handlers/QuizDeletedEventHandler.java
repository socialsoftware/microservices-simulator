package pt.ulisboa.tecnico.socialsoftware.answers.microservices.tournament.events.handling.handlers;

import pt.ulisboa.tecnico.socialsoftware.ms.coordination.eventProcessing.EventProcessingHandler;
import pt.ulisboa.tecnico.socialsoftware.ms.domain.aggregate.Aggregate;
import pt.ulisboa.tecnico.socialsoftware.ms.exception.SimulatorException;
import static pt.ulisboa.tecnico.socialsoftware.ms.exception.SimulatorErrorMessage.AGGREGATE_DELETED;
import pt.ulisboa.tecnico.socialsoftware.answers.microservices.tournament.aggregate.Tournament;
import pt.ulisboa.tecnico.socialsoftware.answers.microservices.tournament.repository.TournamentRepository;
import pt.ulisboa.tecnico.socialsoftware.answers.microservices.quiz.events.publish.QuizDeletedEvent;

import org.springframework.stereotype.Component;
import java.util.List;

@Component
public class QuizDeletedEventHandler implements EventProcessingHandler<QuizDeletedEvent, Tournament> {

    private final TournamentRepository tournamentRepository;

    public QuizDeletedEventHandler(TournamentRepository tournamentRepository) {
        this.tournamentRepository = tournamentRepository;
    }

    @Override
    public void handleEvent(Tournament tournament, QuizDeletedEvent event) {
        // Reference constraint: prevent deletion if references exist
        if (tournament.getQuiz() != null) {
            Integer referencedQuizId = tournament.getQuiz().getQuizAggregateId();
            if (referencedQuizId != null && referencedQuizId.equals(event.getPublisherAggregateId())) {
                throw new SimulatorException(AGGREGATE_DELETED, "Cannot delete quiz that is used in tournaments");
            }
        }
    }
}
