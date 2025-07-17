package pt.ulisboa.tecnico.socialsoftware.quizzes.command.quiz;

import pt.ulisboa.tecnico.socialsoftware.ms.coordination.unitOfWork.UnitOfWork;
import pt.ulisboa.tecnico.socialsoftware.ms.coordination.workflow.Command;
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.quiz.aggregate.QuizDto;
import java.util.List;

public class GenerateQuizCommand extends Command {
    private Integer courseExecutionAggregateId;
    private QuizDto quizDto;
    private List<Integer> topicIds;
    private Integer numberOfQuestions;

    public GenerateQuizCommand(UnitOfWork unitOfWork, String serviceName, Integer courseExecutionAggregateId, QuizDto quizDto, List<Integer> topicIds, Integer numberOfQuestions) {
        super(unitOfWork, serviceName, courseExecutionAggregateId);
        this.courseExecutionAggregateId = courseExecutionAggregateId;
        this.quizDto = quizDto;
        this.topicIds = topicIds;
        this.numberOfQuestions = numberOfQuestions;
    }

    public Integer getCourseExecutionAggregateId() { return courseExecutionAggregateId; }
    public QuizDto getQuizDto() { return quizDto; }
    public List<Integer> getTopicIds() { return topicIds; }
    public Integer getNumberOfQuestions() { return numberOfQuestions; }
}
