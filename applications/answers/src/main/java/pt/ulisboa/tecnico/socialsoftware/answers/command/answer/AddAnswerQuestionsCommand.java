package pt.ulisboa.tecnico.socialsoftware.answers.command.answer;

import pt.ulisboa.tecnico.socialsoftware.ms.coordination.unitOfWork.UnitOfWork;
import pt.ulisboa.tecnico.socialsoftware.ms.coordination.workflow.command.Command;
import pt.ulisboa.tecnico.socialsoftware.answers.shared.dtos.AnswerQuestionDto;
import java.util.List;

public class AddAnswerQuestionsCommand extends Command {
    private final Integer answerId;
    private final List<AnswerQuestionDto> questionDtos;

    public AddAnswerQuestionsCommand(UnitOfWork unitOfWork, String serviceName, Integer answerId, List<AnswerQuestionDto> questionDtos) {
        super(unitOfWork, serviceName, null);
        this.answerId = answerId;
        this.questionDtos = questionDtos;
    }

    public Integer getAnswerId() { return answerId; }
    public List<AnswerQuestionDto> getQuestionDtos() { return questionDtos; }
}
