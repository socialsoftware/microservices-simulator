package pt.ulisboa.tecnico.socialsoftware.quizzes.commands.tournament;

import pt.ulisboa.tecnico.socialsoftware.ms.messaging.Command;
import pt.ulisboa.tecnico.socialsoftware.ms.transactional.unitOfWork.UnitOfWork;
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
