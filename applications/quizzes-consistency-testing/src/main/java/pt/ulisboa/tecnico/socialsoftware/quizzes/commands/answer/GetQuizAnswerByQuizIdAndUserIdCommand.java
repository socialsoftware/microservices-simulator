package pt.ulisboa.tecnico.socialsoftware.quizzes.commands.answer;

import pt.ulisboa.tecnico.socialsoftware.ms.messaging.Command;
import pt.ulisboa.tecnico.socialsoftware.ms.transaction.unitOfWork.UnitOfWork;

public class GetQuizAnswerByQuizIdAndUserIdCommand extends Command {
    private Integer quizAggregateId;
    private Integer userAggregateId;

    public GetQuizAnswerByQuizIdAndUserIdCommand(UnitOfWork unitOfWork, String serviceName, Integer quizAnswerAggregateId, Integer quizAggregateId, Integer userAggregateId) {
        super(unitOfWork, serviceName, quizAnswerAggregateId);
        this.quizAggregateId = quizAggregateId;
        this.userAggregateId = userAggregateId;
    }

    public Integer getQuizAggregateId() { return quizAggregateId; }
    public Integer getUserAggregateId() { return userAggregateId; }
}
