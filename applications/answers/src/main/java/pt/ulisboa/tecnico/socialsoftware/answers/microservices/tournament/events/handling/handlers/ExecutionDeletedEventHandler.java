package pt.ulisboa.tecnico.socialsoftware.answers.microservices.tournament.events.handling.handlers;

import pt.ulisboa.tecnico.socialsoftware.ms.coordination.eventProcessing.EventProcessingHandler;
import pt.ulisboa.tecnico.socialsoftware.ms.domain.aggregate.Aggregate;
import pt.ulisboa.tecnico.socialsoftware.ms.exception.SimulatorException;
import static pt.ulisboa.tecnico.socialsoftware.ms.exception.SimulatorErrorMessage.AGGREGATE_DELETED;
import pt.ulisboa.tecnico.socialsoftware.answers.microservices.tournament.aggregate.Tournament;
import pt.ulisboa.tecnico.socialsoftware.answers.microservices.tournament.repository.TournamentRepository;
import pt.ulisboa.tecnico.socialsoftware.answers.microservices.execution.events.publish.ExecutionDeletedEvent;

import org.springframework.stereotype.Component;
import java.util.List;

@Component
public class ExecutionDeletedEventHandler implements EventProcessingHandler<ExecutionDeletedEvent, Tournament> {

    private final TournamentRepository tournamentRepository;

    public ExecutionDeletedEventHandler(TournamentRepository tournamentRepository) {
        this.tournamentRepository = tournamentRepository;
    }

    @Override
    public void handleEvent(Tournament tournament, ExecutionDeletedEvent event) {
        // Reference constraint: prevent deletion if references exist
        if (tournament.getExecution() != null) {
            Integer referencedExecutionId = tournament.getExecution().getExecutionAggregateId();
            if (referencedExecutionId != null && referencedExecutionId.equals(event.getPublisherAggregateId())) {
                throw new SimulatorException(AGGREGATE_DELETED, "Cannot delete execution that has tournaments");
            }
        }
    }
}
