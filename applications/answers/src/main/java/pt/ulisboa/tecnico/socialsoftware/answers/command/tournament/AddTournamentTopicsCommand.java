package pt.ulisboa.tecnico.socialsoftware.answers.command.tournament;

import pt.ulisboa.tecnico.socialsoftware.ms.coordination.unitOfWork.UnitOfWork;
import pt.ulisboa.tecnico.socialsoftware.ms.coordination.workflow.Command;
import pt.ulisboa.tecnico.socialsoftware.answers.shared.dtos.TournamentTopicDto;
import java.util.List;

public class AddTournamentTopicsCommand extends Command {
    private final Integer tournamentId;
    private final List<TournamentTopicDto> topicDtos;

    public AddTournamentTopicsCommand(UnitOfWork unitOfWork, String serviceName, Integer tournamentId, List<TournamentTopicDto> topicDtos) {
        super(unitOfWork, serviceName, null);
        this.tournamentId = tournamentId;
        this.topicDtos = topicDtos;
    }

    public Integer getTournamentId() { return tournamentId; }
    public List<TournamentTopicDto> getTopicDtos() { return topicDtos; }
}
