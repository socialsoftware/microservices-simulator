package pt.ulisboa.tecnico.socialsoftware.quizzes.command.quiz;

import pt.ulisboa.tecnico.socialsoftware.ms.coordination.unitOfWork.UnitOfWork;
import pt.ulisboa.tecnico.socialsoftware.ms.coordination.workflow.Command;
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.question.aggregate.QuestionDto;
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.quiz.aggregate.QuizDto;

import java.util.List;

public class GenerateQuizCommand extends Command {
    private Integer courseExecutionAggregateId;
    private QuizDto quizDto;
    private List<QuestionDto> questionDtos;
    private Integer numberOfQuestions;

    public GenerateQuizCommand(UnitOfWork unitOfWork, String serviceName, Integer courseExecutionAggregateId, QuizDto quizDto, List<QuestionDto> questionDtos, Integer numberOfQuestions) {
        super(unitOfWork, serviceName, courseExecutionAggregateId);
        this.courseExecutionAggregateId = courseExecutionAggregateId;
        this.quizDto = quizDto;
        this.questionDtos = questionDtos;
        this.numberOfQuestions = numberOfQuestions;
    }

    public Integer getCourseExecutionAggregateId() { return courseExecutionAggregateId; }
    public QuizDto getQuizDto() { return quizDto; }
    public Integer getNumberOfQuestions() { return numberOfQuestions; }
    public List<QuestionDto> getQuestionDtos() {
        return questionDtos;
    }
}
