package pt.ulisboa.tecnico.socialsoftware.answers.command.quiz;

import pt.ulisboa.tecnico.socialsoftware.ms.coordination.unitOfWork.UnitOfWork;
import pt.ulisboa.tecnico.socialsoftware.ms.coordination.workflow.command.Command;
import pt.ulisboa.tecnico.socialsoftware.answers.shared.dtos.QuizQuestionDto;
import java.util.List;

public class AddQuizQuestionsCommand extends Command {
    private final Integer quizId;
    private final List<QuizQuestionDto> questionDtos;

    public AddQuizQuestionsCommand(UnitOfWork unitOfWork, String serviceName, Integer quizId, List<QuizQuestionDto> questionDtos) {
        super(unitOfWork, serviceName, null);
        this.quizId = quizId;
        this.questionDtos = questionDtos;
    }

    public Integer getQuizId() { return quizId; }
    public List<QuizQuestionDto> getQuestionDtos() { return questionDtos; }
}
