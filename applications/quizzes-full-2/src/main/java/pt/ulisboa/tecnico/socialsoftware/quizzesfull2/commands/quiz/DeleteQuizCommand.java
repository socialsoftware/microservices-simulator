package pt.ulisboa.tecnico.socialsoftware.quizzesfull2.commands.quiz;

import pt.ulisboa.tecnico.socialsoftware.ms.messaging.Command;
import pt.ulisboa.tecnico.socialsoftware.ms.transaction.unitOfWork.UnitOfWork;

public class DeleteQuizCommand extends Command {
    private Integer quizAggregateId;

    protected DeleteQuizCommand() {}

    public DeleteQuizCommand(UnitOfWork unitOfWork, String serviceName, Integer quizAggregateId) {
        super(unitOfWork, serviceName, quizAggregateId);
        this.quizAggregateId = quizAggregateId;
    }

    public Integer getQuizAggregateId() {
        return quizAggregateId;
    }

    public void setQuizAggregateId(Integer quizAggregateId) {
        this.quizAggregateId = quizAggregateId;
    }
}
