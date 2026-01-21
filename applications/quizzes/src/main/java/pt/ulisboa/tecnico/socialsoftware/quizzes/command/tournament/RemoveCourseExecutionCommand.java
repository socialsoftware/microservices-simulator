package pt.ulisboa.tecnico.socialsoftware.quizzes.command.tournament;

import pt.ulisboa.tecnico.socialsoftware.ms.coordination.unitOfWork.UnitOfWork;
import pt.ulisboa.tecnico.socialsoftware.ms.coordination.workflow.command.Command;

public class RemoveCourseExecutionCommand extends Command {
    private final Integer tournamentAggregateId;
    private final Integer courseExecutionId;
    private final Integer eventVersion;

    public RemoveCourseExecutionCommand(UnitOfWork unitOfWork, String serviceName, Integer tournamentAggregateId,
            Integer courseExecutionId, Integer eventVersion) {
        super(unitOfWork, serviceName, tournamentAggregateId);
        this.tournamentAggregateId = tournamentAggregateId;
        this.courseExecutionId = courseExecutionId;
        this.eventVersion = eventVersion;
    }

    public Integer getTournamentAggregateId() {
        return tournamentAggregateId;
    }

    public Integer getCourseExecutionId() {
        return courseExecutionId;
    }

    public Integer getEventVersion() {
        return eventVersion;
    }
}
