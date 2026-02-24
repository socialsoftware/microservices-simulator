package pt.ulisboa.tecnico.socialsoftware.answers.command.question;

import pt.ulisboa.tecnico.socialsoftware.ms.coordination.unitOfWork.UnitOfWork;
import pt.ulisboa.tecnico.socialsoftware.ms.coordination.workflow.Command;

public class GetQuestionOptionCommand extends Command {
    private final Integer questionId;
    private final Integer key;

    public GetQuestionOptionCommand(UnitOfWork unitOfWork, String serviceName, Integer questionId, Integer key) {
        super(unitOfWork, serviceName, null);
        this.questionId = questionId;
        this.key = key;
    }

    public Integer getQuestionId() { return questionId; }
    public Integer getKey() { return key; }
}
