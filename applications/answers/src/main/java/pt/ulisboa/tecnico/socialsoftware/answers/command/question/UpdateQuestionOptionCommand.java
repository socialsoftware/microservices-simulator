package pt.ulisboa.tecnico.socialsoftware.answers.command.question;

import pt.ulisboa.tecnico.socialsoftware.ms.coordination.unitOfWork.UnitOfWork;
import pt.ulisboa.tecnico.socialsoftware.ms.coordination.workflow.command.Command;
import pt.ulisboa.tecnico.socialsoftware.answers.shared.dtos.OptionDto;

public class UpdateQuestionOptionCommand extends Command {
    private final Integer questionId;
    private final Integer key;
    private final OptionDto optionDto;

    public UpdateQuestionOptionCommand(UnitOfWork unitOfWork, String serviceName, Integer questionId, Integer key, OptionDto optionDto) {
        super(unitOfWork, serviceName, null);
        this.questionId = questionId;
        this.key = key;
        this.optionDto = optionDto;
    }

    public Integer getQuestionId() { return questionId; }
    public Integer getKey() { return key; }
    public OptionDto getOptionDto() { return optionDto; }
}
