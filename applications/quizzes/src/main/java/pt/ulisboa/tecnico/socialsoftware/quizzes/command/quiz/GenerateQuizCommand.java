package pt.ulisboa.tecnico.socialsoftware.quizzes.command.quiz;

import pt.ulisboa.tecnico.socialsoftware.ms.coordination.unitOfWork.UnitOfWork;
import pt.ulisboa.tecnico.socialsoftware.ms.coordination.workflow.command.Command;
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.execution.aggregate.CourseExecutionDto;
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.question.aggregate.QuestionDto;
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.quiz.aggregate.QuizDto;

import java.util.List;

public class GenerateQuizCommand extends Command {
    private CourseExecutionDto courseExecutionDto;
    private QuizDto quizDto;
    private List<QuestionDto> questionDtos;
    private Integer numberOfQuestions;

    public GenerateQuizCommand(UnitOfWork unitOfWork, String serviceName, CourseExecutionDto courseExecutionDto, QuizDto quizDto, List<QuestionDto> questionDtos, Integer numberOfQuestions) {
        super(unitOfWork, serviceName, null);
        this.courseExecutionDto = courseExecutionDto;
        this.quizDto = quizDto;
        this.questionDtos = questionDtos;
        this.numberOfQuestions = numberOfQuestions;
    }

    public QuizDto getQuizDto() { return quizDto; }
    public Integer getNumberOfQuestions() { return numberOfQuestions; }
    public List<QuestionDto> getQuestionDtos() {
        return questionDtos;
    }

    public CourseExecutionDto getCourseExecutionDto() {
        return courseExecutionDto;
    }
}
