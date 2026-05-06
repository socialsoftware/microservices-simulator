package pt.ulisboa.tecnico.socialsoftware.quizzes.commands.quiz;

import pt.ulisboa.tecnico.socialsoftware.ms.messaging.Command;
import pt.ulisboa.tecnico.socialsoftware.ms.transaction.unitOfWork.UnitOfWork;

public class RemoveCourseExecutionCommand extends Command {
    private final Integer quizAggregateId;
    private final Integer courseExecutionId;
    private final Long aggregateVersion;

    public RemoveCourseExecutionCommand(UnitOfWork unitOfWork,
            String serviceName,
            Integer quizAggregateId,
            Integer courseExecutionId,
            Long aggregateVersion) {
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

    public Long getAggregateVersion() {
        return aggregateVersion;
    }
}
