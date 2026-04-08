package pt.ulisboa.tecnico.socialsoftware.quizzes.command.tournament;

import pt.ulisboa.tecnico.socialsoftware.ms.messaging.Command;
import pt.ulisboa.tecnico.socialsoftware.ms.transactional.unitOfWork.UnitOfWork;

public class RemoveUserCommand extends Command {
    private final Integer tournamentAggregateId;
    private final Integer courseExecutionAggregateId;
    private final Integer userAggregateId;
    private final Long eventVersion;

    public RemoveUserCommand(UnitOfWork unitOfWork, String serviceName, Integer tournamentAggregateId,
            Integer courseExecutionAggregateId, Integer userAggregateId, Long eventVersion) {
        super(unitOfWork, serviceName, tournamentAggregateId);
        this.tournamentAggregateId = tournamentAggregateId;
        this.courseExecutionAggregateId = courseExecutionAggregateId;
        this.userAggregateId = userAggregateId;
        this.eventVersion = eventVersion;
    }

    public Integer getTournamentAggregateId() {
        return tournamentAggregateId;
    }

    public Integer getCourseExecutionAggregateId() {
        return courseExecutionAggregateId;
    }

    public Integer getUserAggregateId() {
        return userAggregateId;
    }

    public Long getEventVersion() {
        return eventVersion;
    }
}
