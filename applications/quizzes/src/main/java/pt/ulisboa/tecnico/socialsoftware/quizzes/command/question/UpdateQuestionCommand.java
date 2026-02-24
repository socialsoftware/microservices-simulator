package pt.ulisboa.tecnico.socialsoftware.quizzes.command.question;

import pt.ulisboa.tecnico.socialsoftware.ms.coordination.unitOfWork.UnitOfWork;
import pt.ulisboa.tecnico.socialsoftware.ms.coordination.workflow.Command;
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.question.aggregate.QuestionDto;

public class UpdateQuestionCommand extends Command {
    private QuestionDto questionDto;

    public UpdateQuestionCommand(UnitOfWork unitOfWork, String serviceName, QuestionDto questionDto) {
        super(unitOfWork, serviceName, questionDto.getAggregateId());
        this.questionDto = questionDto;
    }

    public QuestionDto getQuestionDto() {
        return questionDto;
    }
}
