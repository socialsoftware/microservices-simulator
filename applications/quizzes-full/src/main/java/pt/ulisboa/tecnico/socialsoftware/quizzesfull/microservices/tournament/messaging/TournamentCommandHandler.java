package pt.ulisboa.tecnico.socialsoftware.quizzesfull.microservices.tournament.messaging;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import pt.ulisboa.tecnico.socialsoftware.ms.messaging.Command;
import pt.ulisboa.tecnico.socialsoftware.ms.messaging.CommandHandler;
import pt.ulisboa.tecnico.socialsoftware.quizzesfull.commands.tournament.AddParticipantCommand;
import pt.ulisboa.tecnico.socialsoftware.quizzesfull.commands.tournament.CancelTournamentCommand;
import pt.ulisboa.tecnico.socialsoftware.quizzesfull.commands.tournament.CreateTournamentCommand;
import pt.ulisboa.tecnico.socialsoftware.quizzesfull.commands.tournament.DeleteTournamentCommand;
import pt.ulisboa.tecnico.socialsoftware.quizzesfull.commands.tournament.GetOpenTournamentsCommand;
import pt.ulisboa.tecnico.socialsoftware.quizzesfull.commands.tournament.GetTournamentByIdCommand;
import pt.ulisboa.tecnico.socialsoftware.quizzesfull.commands.tournament.UpdateTournamentCommand;
import pt.ulisboa.tecnico.socialsoftware.quizzesfull.microservices.tournament.service.TournamentService;

import java.util.logging.Logger;

@Component
public class TournamentCommandHandler extends CommandHandler {
    private static final Logger logger = Logger.getLogger(TournamentCommandHandler.class.getName());

    @Autowired
    private TournamentService tournamentService;

    @Override
    public String getAggregateTypeName() {
        return "Tournament";
    }

    @Override
    public Object handleDomainCommand(Command command) {
        return switch (command) {
            case GetTournamentByIdCommand cmd -> tournamentService.getTournamentById(
                    cmd.getTournamentAggregateId(), cmd.getUnitOfWork());
            case GetOpenTournamentsCommand cmd -> tournamentService.getOpenTournaments(
                    cmd.getExecutionAggregateId(), cmd.getUnitOfWork());
            case CreateTournamentCommand cmd -> tournamentService.createTournament(
                    cmd.getExecutionAggregateId(), cmd.getExecutionVersion(), cmd.getExecutionCourseId(),
                    cmd.getCreatorAggregateId(), cmd.getCreatorName(), cmd.getCreatorUsername(), cmd.getCreatorVersion(),
                    cmd.getTopicDtos(),
                    cmd.getQuizAggregateId(), cmd.getQuizVersion(),
                    cmd.getStartTime(), cmd.getEndTime(), cmd.getNumberOfQuestions(),
                    cmd.getUnitOfWork());
            case AddParticipantCommand cmd -> {
                tournamentService.addParticipant(
                        cmd.getTournamentAggregateId(),
                        cmd.getUserAggregateId(), cmd.getUserName(),
                        cmd.getUserUsername(), cmd.getUserVersion(),
                        cmd.getUnitOfWork());
                yield null;
            }
            case UpdateTournamentCommand cmd -> {
                tournamentService.updateTournament(
                        cmd.getTournamentAggregateId(),
                        cmd.getStartTime(), cmd.getEndTime(), cmd.getTopicDtos(),
                        cmd.getUnitOfWork());
                yield null;
            }
            case CancelTournamentCommand cmd -> {
                tournamentService.cancelTournament(cmd.getTournamentAggregateId(), cmd.getUnitOfWork());
                yield null;
            }
            case DeleteTournamentCommand cmd -> {
                tournamentService.deleteTournament(cmd.getTournamentAggregateId(), cmd.getUnitOfWork());
                yield null;
            }
            default -> {
                logger.warning("Unknown command type: " + command.getClass().getName());
                yield null;
            }
        };
    }
}
