package pt.ulisboa.tecnico.socialsoftware.quizzesfull2.commands.quizanswer;

import pt.ulisboa.tecnico.socialsoftware.ms.messaging.Command;
import pt.ulisboa.tecnico.socialsoftware.ms.transaction.unitOfWork.UnitOfWork;
import pt.ulisboa.tecnico.socialsoftware.quizzesfull2.microservices.execution.aggregate.ExecutionDto;
import pt.ulisboa.tecnico.socialsoftware.quizzesfull2.microservices.quiz.aggregate.QuizDto;
import pt.ulisboa.tecnico.socialsoftware.quizzesfull2.microservices.quizanswer.aggregate.QuestionAnswerDto;
import pt.ulisboa.tecnico.socialsoftware.quizzesfull2.microservices.user.aggregate.UserDto;

import java.util.List;

public class CreateQuizAnswerCommand extends Command {
    private QuizDto quizDto;
    private UserDto userDto;
    private ExecutionDto executionDto;
    private List<QuestionAnswerDto> questionAnswers;

    protected CreateQuizAnswerCommand() {}

    public CreateQuizAnswerCommand(UnitOfWork unitOfWork, String serviceName, QuizDto quizDto, UserDto userDto,
                                   ExecutionDto executionDto, List<QuestionAnswerDto> questionAnswers) {
        super(unitOfWork, serviceName, null);
        this.quizDto = quizDto;
        this.userDto = userDto;
        this.executionDto = executionDto;
        this.questionAnswers = questionAnswers;
    }

    public QuizDto getQuizDto() {
        return quizDto;
    }

    public void setQuizDto(QuizDto quizDto) {
        this.quizDto = quizDto;
    }

    public UserDto getUserDto() {
        return userDto;
    }

    public void setUserDto(UserDto userDto) {
        this.userDto = userDto;
    }

    public ExecutionDto getExecutionDto() {
        return executionDto;
    }

    public void setExecutionDto(ExecutionDto executionDto) {
        this.executionDto = executionDto;
    }

    public List<QuestionAnswerDto> getQuestionAnswers() {
        return questionAnswers;
    }

    public void setQuestionAnswers(List<QuestionAnswerDto> questionAnswers) {
        this.questionAnswers = questionAnswers;
    }
}
