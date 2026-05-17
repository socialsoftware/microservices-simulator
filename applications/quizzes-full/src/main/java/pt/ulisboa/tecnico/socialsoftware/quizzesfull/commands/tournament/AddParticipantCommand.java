package pt.ulisboa.tecnico.socialsoftware.quizzesfull.commands.tournament;

import pt.ulisboa.tecnico.socialsoftware.ms.messaging.Command;
import pt.ulisboa.tecnico.socialsoftware.ms.transaction.unitOfWork.UnitOfWork;

public class AddParticipantCommand extends Command {
    private final Integer tournamentAggregateId;
    private final Integer userAggregateId;
    private final String userName;
    private final String userUsername;
    private final Long userVersion;

    public AddParticipantCommand(UnitOfWork unitOfWork, String serviceName,
                                  Integer tournamentAggregateId,
                                  Integer userAggregateId, String userName,
                                  String userUsername, Long userVersion) {
        super(unitOfWork, serviceName, tournamentAggregateId);
        this.tournamentAggregateId = tournamentAggregateId;
        this.userAggregateId = userAggregateId;
        this.userName = userName;
        this.userUsername = userUsername;
        this.userVersion = userVersion;
    }

    public Integer getTournamentAggregateId() { return tournamentAggregateId; }
    public Integer getUserAggregateId() { return userAggregateId; }
    public String getUserName() { return userName; }
    public String getUserUsername() { return userUsername; }
    public Long getUserVersion() { return userVersion; }
}
