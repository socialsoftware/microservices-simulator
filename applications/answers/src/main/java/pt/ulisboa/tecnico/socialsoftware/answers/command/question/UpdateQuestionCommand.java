package pt.ulisboa.tecnico.socialsoftware.answers.command.question;

import pt.ulisboa.tecnico.socialsoftware.ms.coordination.unitOfWork.UnitOfWork;
import pt.ulisboa.tecnico.socialsoftware.ms.coordination.workflow.command.Command;
import pt.ulisboa.tecnico.socialsoftware.answers.shared.dtos.QuestionDto;

public class UpdateQuestionCommand extends Command {
    private final QuestionDto questionDto;

    public UpdateQuestionCommand(UnitOfWork unitOfWork, String serviceName, QuestionDto questionDto) {
        super(unitOfWork, serviceName, null);
        this.questionDto = questionDto;
    }

    public QuestionDto getQuestionDto() { return questionDto; }
}
