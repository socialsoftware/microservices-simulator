package pt.ulisboa.tecnico.socialsoftware.quizzesfull2.commands.quizanswer;

import pt.ulisboa.tecnico.socialsoftware.ms.messaging.Command;
import pt.ulisboa.tecnico.socialsoftware.ms.transaction.unitOfWork.UnitOfWork;

public class GetQuizAnswerForStudentAndQuizCommand extends Command {
    private Integer userAggregateId;
    private Integer quizAggregateId;

    protected GetQuizAnswerForStudentAndQuizCommand() {}

    public GetQuizAnswerForStudentAndQuizCommand(UnitOfWork unitOfWork, String serviceName,
                                                 Integer userAggregateId, Integer quizAggregateId) {
        super(unitOfWork, serviceName, null);
        this.userAggregateId = userAggregateId;
        this.quizAggregateId = quizAggregateId;
    }

    public Integer getUserAggregateId() {
        return userAggregateId;
    }

    public void setUserAggregateId(Integer userAggregateId) {
        this.userAggregateId = userAggregateId;
    }

    public Integer getQuizAggregateId() {
        return quizAggregateId;
    }

    public void setQuizAggregateId(Integer quizAggregateId) {
        this.quizAggregateId = quizAggregateId;
    }
}
