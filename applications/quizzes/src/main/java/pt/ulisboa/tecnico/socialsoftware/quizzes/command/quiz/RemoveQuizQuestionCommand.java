package pt.ulisboa.tecnico.socialsoftware.quizzes.command.quiz;

import pt.ulisboa.tecnico.socialsoftware.ms.coordination.unitOfWork.UnitOfWork;
import pt.ulisboa.tecnico.socialsoftware.ms.coordination.workflow.command.Command;

public class RemoveQuizQuestionCommand extends Command {
    private final Integer quizAggregateId;
    private final Integer questionAggregateId;

    public RemoveQuizQuestionCommand(UnitOfWork unitOfWork,
            String serviceName,
            Integer quizAggregateId,
            Integer questionAggregateId) {
        super(unitOfWork, serviceName, quizAggregateId);
        this.quizAggregateId = quizAggregateId;
        this.questionAggregateId = questionAggregateId;
    }

    public Integer getQuizAggregateId() {
        return quizAggregateId;
    }

    public Integer getQuestionAggregateId() {
        return questionAggregateId;
    }
}
