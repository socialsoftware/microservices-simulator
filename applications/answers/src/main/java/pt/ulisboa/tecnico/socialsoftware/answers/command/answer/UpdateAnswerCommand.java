package pt.ulisboa.tecnico.socialsoftware.answers.command.answer;

import pt.ulisboa.tecnico.socialsoftware.ms.coordination.unitOfWork.UnitOfWork;
import pt.ulisboa.tecnico.socialsoftware.ms.coordination.workflow.command.Command;
import pt.ulisboa.tecnico.socialsoftware.answers.shared.dtos.AnswerDto;

public class UpdateAnswerCommand extends Command {
    private final AnswerDto answerDto;

    public UpdateAnswerCommand(UnitOfWork unitOfWork, String serviceName, AnswerDto answerDto) {
        super(unitOfWork, serviceName, null);
        this.answerDto = answerDto;
    }

    public AnswerDto getAnswerDto() { return answerDto; }
}
