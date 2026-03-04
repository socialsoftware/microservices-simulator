package pt.ulisboa.tecnico.socialsoftware.answers.command.tournament;

import pt.ulisboa.tecnico.socialsoftware.ms.coordination.unitOfWork.UnitOfWork;
import pt.ulisboa.tecnico.socialsoftware.ms.coordination.workflow.Command;
import pt.ulisboa.tecnico.socialsoftware.answers.shared.dtos.TournamentTopicDto;

public class AddTournamentTopicCommand extends Command {
    private final Integer tournamentId;
    private final Integer topicAggregateId;
    private final TournamentTopicDto topicDto;

    public AddTournamentTopicCommand(UnitOfWork unitOfWork, String serviceName, Integer tournamentId, Integer topicAggregateId, TournamentTopicDto topicDto) {
        super(unitOfWork, serviceName, null);
        this.tournamentId = tournamentId;
        this.topicAggregateId = topicAggregateId;
        this.topicDto = topicDto;
    }

    public Integer getTournamentId() { return tournamentId; }
    public Integer getTopicAggregateId() { return topicAggregateId; }
    public TournamentTopicDto getTopicDto() { return topicDto; }
}
