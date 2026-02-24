package pt.ulisboa.tecnico.socialsoftware.quizzes.command.tournament;

import pt.ulisboa.tecnico.socialsoftware.ms.coordination.unitOfWork.UnitOfWork;
import pt.ulisboa.tecnico.socialsoftware.ms.coordination.workflow.command.Command;

public class RemoveUserCommand extends Command {
    private final Integer tournamentAggregateId;
    private final Integer courseExecutionAggregateId;
    private final Integer userAggregateId;
    private final Integer eventVersion;

    public RemoveUserCommand(UnitOfWork unitOfWork, String serviceName, Integer tournamentAggregateId,
            Integer courseExecutionAggregateId, Integer userAggregateId, Integer eventVersion) {
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

    public Integer getEventVersion() {
        return eventVersion;
    }
}
