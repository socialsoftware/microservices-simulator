package pt.ulisboa.tecnico.socialsoftware.quizzes.command.answer;

import pt.ulisboa.tecnico.socialsoftware.ms.coordination.unitOfWork.UnitOfWork;
import pt.ulisboa.tecnico.socialsoftware.ms.coordination.workflow.Command;

public class GetQuizAnswerDtoByQuizIdAndUserIdCommand extends Command {
    private Integer quizAggregateId;
    private Integer userAggregateId;

    public GetQuizAnswerDtoByQuizIdAndUserIdCommand(UnitOfWork unitOfWork, String serviceName, Integer quizAnswerAggregateId, Integer quizAggregateId, Integer userAggregateId) {
        super(unitOfWork, serviceName, quizAnswerAggregateId);
        this.quizAggregateId = quizAggregateId;
        this.userAggregateId = userAggregateId;
    }

    public Integer getQuizAggregateId() { return quizAggregateId; }
    public Integer getUserAggregateId() { return userAggregateId; }
}
