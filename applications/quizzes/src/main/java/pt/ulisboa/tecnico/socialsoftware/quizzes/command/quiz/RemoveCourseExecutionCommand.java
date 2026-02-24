package pt.ulisboa.tecnico.socialsoftware.quizzes.command.quiz;

import pt.ulisboa.tecnico.socialsoftware.ms.coordination.unitOfWork.UnitOfWork;
import pt.ulisboa.tecnico.socialsoftware.ms.coordination.workflow.command.Command;

public class RemoveCourseExecutionCommand extends Command {
    private final Integer quizAggregateId;
    private final Integer courseExecutionId;
    private final Integer aggregateVersion;

    public RemoveCourseExecutionCommand(UnitOfWork unitOfWork,
            String serviceName,
            Integer quizAggregateId,
            Integer courseExecutionId,
            Integer aggregateVersion) {
        super(unitOfWork, serviceName, quizAggregateId);
        this.quizAggregateId = quizAggregateId;
        this.courseExecutionId = courseExecutionId;
        this.aggregateVersion = aggregateVersion;
    }

    public Integer getQuizAggregateId() {
        return quizAggregateId;
    }

    public Integer getCourseExecutionId() {
        return courseExecutionId;
    }

    public Integer getAggregateVersion() {
        return aggregateVersion;
    }
}
