package pt.ulisboa.tecnico.socialsoftware.quizzes.commands.answer;

import pt.ulisboa.tecnico.socialsoftware.ms.messaging.Command;
import pt.ulisboa.tecnico.socialsoftware.ms.transactional.unitOfWork.UnitOfWork;

public class RemoveQuizAnswerCommand extends Command {
    private Integer quizAnswerAggregateId;

    public RemoveQuizAnswerCommand(UnitOfWork unitOfWork, String serviceName, Integer quizAnswerAggregateId) {
        super(unitOfWork, serviceName, quizAnswerAggregateId);
        this.quizAnswerAggregateId = quizAnswerAggregateId;
    }

    public Integer getQuizAnswerAggregateId() { return quizAnswerAggregateId; }
}
