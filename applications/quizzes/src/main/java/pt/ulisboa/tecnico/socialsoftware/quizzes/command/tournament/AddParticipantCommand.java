package pt.ulisboa.tecnico.socialsoftware.quizzes.command.tournament;

import pt.ulisboa.tecnico.socialsoftware.ms.coordination.unitOfWork.UnitOfWork;
import pt.ulisboa.tecnico.socialsoftware.ms.coordination.workflow.command.Command;
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.user.aggregate.UserDto;

public class AddParticipantCommand extends Command {
    private Integer tournamentAggregateId;
    private UserDto userDto;

    public AddParticipantCommand(UnitOfWork unitOfWork, String serviceName, Integer tournamentAggregateId, UserDto userDto) {
        super(unitOfWork, serviceName, tournamentAggregateId);
        this.tournamentAggregateId = tournamentAggregateId;
        this.userDto = userDto;
    }

    public Integer getTournamentAggregateId() {
        return tournamentAggregateId;
    }

    public UserDto getUserDto() {
        return userDto;
    }

}
