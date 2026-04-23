package pt.ulisboa.tecnico.socialsoftware.answers.command.question;

import pt.ulisboa.tecnico.socialsoftware.ms.coordination.unitOfWork.UnitOfWork;
import pt.ulisboa.tecnico.socialsoftware.ms.coordination.workflow.command.Command;
import pt.ulisboa.tecnico.socialsoftware.answers.shared.dtos.OptionDto;
import java.util.List;

public class AddQuestionOptionsCommand extends Command {
    private final Integer questionId;
    private final List<OptionDto> optionDtos;

    public AddQuestionOptionsCommand(UnitOfWork unitOfWork, String serviceName, Integer questionId, List<OptionDto> optionDtos) {
        super(unitOfWork, serviceName, null);
        this.questionId = questionId;
        this.optionDtos = optionDtos;
    }

    public Integer getQuestionId() { return questionId; }
    public List<OptionDto> getOptionDtos() { return optionDtos; }
}
