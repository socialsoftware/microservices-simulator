package pt.ulisboa.tecnico.socialsoftware.quizzes.commands.question;

import pt.ulisboa.tecnico.socialsoftware.ms.messaging.Command;
import pt.ulisboa.tecnico.socialsoftware.ms.transactional.unitOfWork.UnitOfWork;
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
