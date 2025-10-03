package pt.ulisboa.tecnico.socialsoftware.answers.microservices.tournament.aggregate;

import org.springframework.stereotype.Service;
import pt.ulisboa.tecnico.socialsoftware.ms.causal.aggregate.CausalAggregate;
import pt.ulisboa.tecnico.socialsoftware.ms.coordination.unitOfWork.UnitOfWork;
import pt.ulisboa.tecnico.socialsoftware.ms.coordination.unitOfWork.UnitOfWorkService;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.aggregate.SagaAggregate;

@Service
public class TournamentFactory {

    public Tournament createTournament(Integer aggregateId, TournamentDto tournamentDto) {
        // Factory method implementation - create root entity directly
        // Extract properties from DTO and create the root entity
        return new Tournament(
            tournamentDto.getStartTime(),
            tournamentDto.getEndTime(),
            tournamentDto.getNumberOfQuestions(),
            tournamentDto.getCancelled(),
            tournamentDto.getTournamentCreator(),
            tournamentDto.getTournamentParticipants(),
            tournamentDto.getTournamentExecution(),
            tournamentDto.getTournamentTopics(),
            tournamentDto.getTournamentQuiz()
        );
    }

    public Tournament createTournamentFromExisting(Tournament existingTournament) {
        // Create a copy of the existing aggregate
        if (existingTournament instanceof Tournament) {
            return new Tournament((Tournament) existingTournament);
        }
        throw new IllegalArgumentException("Unknown aggregate type");
    }

    public TournamentDto createTournamentDto(Tournament tournament) {
        return new TournamentDto((Tournament) tournament);
    }
}