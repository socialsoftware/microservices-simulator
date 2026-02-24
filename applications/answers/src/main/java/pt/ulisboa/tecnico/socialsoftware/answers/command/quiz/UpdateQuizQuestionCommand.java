package pt.ulisboa.tecnico.socialsoftware.answers.command.quiz;

import pt.ulisboa.tecnico.socialsoftware.ms.coordination.unitOfWork.UnitOfWork;
import pt.ulisboa.tecnico.socialsoftware.ms.coordination.workflow.Command;
import pt.ulisboa.tecnico.socialsoftware.answers.shared.dtos.QuizQuestionDto;

public class UpdateQuizQuestionCommand extends Command {
    private final Integer quizId;
    private final Integer questionAggregateId;
    private final QuizQuestionDto questionDto;

    public UpdateQuizQuestionCommand(UnitOfWork unitOfWork, String serviceName, Integer quizId, Integer questionAggregateId, QuizQuestionDto questionDto) {
        super(unitOfWork, serviceName, null);
        this.quizId = quizId;
        this.questionAggregateId = questionAggregateId;
        this.questionDto = questionDto;
    }

    public Integer getQuizId() { return quizId; }
    public Integer getQuestionAggregateId() { return questionAggregateId; }
    public QuizQuestionDto getQuestionDto() { return questionDto; }
}
