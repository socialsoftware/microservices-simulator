package pt.ulisboa.tecnico.socialsoftware.quizzesfull2.commands.quiz;

import pt.ulisboa.tecnico.socialsoftware.ms.messaging.Command;
import pt.ulisboa.tecnico.socialsoftware.ms.transaction.unitOfWork.UnitOfWork;
import pt.ulisboa.tecnico.socialsoftware.quizzesfull2.microservices.execution.aggregate.ExecutionDto;
import pt.ulisboa.tecnico.socialsoftware.quizzesfull2.microservices.quiz.aggregate.QuizDto;
import pt.ulisboa.tecnico.socialsoftware.quizzesfull2.microservices.quiz.aggregate.QuizQuestionDto;

import java.util.List;

public class CreateQuizCommand extends Command {
    private QuizDto quizDto;
    private ExecutionDto executionDto;
    private List<QuizQuestionDto> questions;

    protected CreateQuizCommand() {}

    public CreateQuizCommand(UnitOfWork unitOfWork, String serviceName, QuizDto quizDto,
                             ExecutionDto executionDto, List<QuizQuestionDto> questions) {
        super(unitOfWork, serviceName, null);
        this.quizDto = quizDto;
        this.executionDto = executionDto;
        this.questions = questions;
    }

    public QuizDto getQuizDto() {
        return quizDto;
    }

    public void setQuizDto(QuizDto quizDto) {
        this.quizDto = quizDto;
    }

    public ExecutionDto getExecutionDto() {
        return executionDto;
    }

    public void setExecutionDto(ExecutionDto executionDto) {
        this.executionDto = executionDto;
    }

    public List<QuizQuestionDto> getQuestions() {
        return questions;
    }

    public void setQuestions(List<QuizQuestionDto> questions) {
        this.questions = questions;
    }
}
