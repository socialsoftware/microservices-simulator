package pt.ulisboa.tecnico.socialsoftware.quizzesfull.commands.quizanswer;

import pt.ulisboa.tecnico.socialsoftware.ms.messaging.Command;
import pt.ulisboa.tecnico.socialsoftware.ms.transaction.unitOfWork.UnitOfWork;

public class GetQuizAnswerByQuizIdAndStudentIdCommand extends Command {
    private final Integer quizAggregateId;
    private final Integer userAggregateId;

    public GetQuizAnswerByQuizIdAndStudentIdCommand(UnitOfWork unitOfWork, String serviceName,
                                                     Integer quizAggregateId, Integer userAggregateId) {
        super(unitOfWork, serviceName, quizAggregateId);
        this.quizAggregateId = quizAggregateId;
        this.userAggregateId = userAggregateId;
    }

    public Integer getQuizAggregateId() { return quizAggregateId; }
    public Integer getUserAggregateId() { return userAggregateId; }
}
