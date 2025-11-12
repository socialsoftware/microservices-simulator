package pt.ulisboa.tecnico.socialsoftware.quizzes.command.answer;

import pt.ulisboa.tecnico.socialsoftware.ms.coordination.unitOfWork.UnitOfWork;
import pt.ulisboa.tecnico.socialsoftware.ms.coordination.workflow.Command;
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.quiz.aggregate.QuizDto;
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.user.aggregate.UserDto;

public class StartQuizCommand extends Command {
    private Integer quizAggregateId;
    private Integer courseExecutionAggregateId;
    private final QuizDto quizDto;
    private final UserDto userDto;

    public StartQuizCommand(UnitOfWork unitOfWork, String serviceName, Integer quizAggregateId, Integer courseExecutionAggregateId, QuizDto quizDto, UserDto userDto) {
        super(unitOfWork, serviceName, quizAggregateId);
        this.quizAggregateId = quizAggregateId;
        this.courseExecutionAggregateId = courseExecutionAggregateId;
        this.quizDto = quizDto;
        this.userDto = userDto;
    }

    public Integer getQuizAggregateId() { return quizAggregateId; }

    public QuizDto getQuizDto() {
        return quizDto;
    }

    public UserDto getUserDto() {
        return userDto;
    }

    public Integer getCourseExecutionAggregateId() {
        return courseExecutionAggregateId;
    }
}
