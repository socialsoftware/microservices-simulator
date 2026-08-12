package pt.ulisboa.tecnico.socialsoftware.answers.command.quiz;

import pt.ulisboa.tecnico.socialsoftware.ms.coordination.unitOfWork.UnitOfWork;
import pt.ulisboa.tecnico.socialsoftware.ms.coordination.workflow.command.Command;

public class RemoveQuizQuestionCommand extends Command {
    private final Integer quizId;
    private final Integer questionAggregateId;

    public RemoveQuizQuestionCommand(UnitOfWork unitOfWork, String serviceName, Integer quizId, Integer questionAggregateId) {
        super(unitOfWork, serviceName, null);
        this.quizId = quizId;
        this.questionAggregateId = questionAggregateId;
    }

    public Integer getQuizId() { return quizId; }
    public Integer getQuestionAggregateId() { return questionAggregateId; }
}
