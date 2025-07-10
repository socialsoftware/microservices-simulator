package pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.execution.commandHandler;

import org.springframework.stereotype.Component;
import pt.ulisboa.tecnico.socialsoftware.ms.coordination.workflow.Command;
import pt.ulisboa.tecnico.socialsoftware.ms.coordination.workflow.CommandHandler;
import pt.ulisboa.tecnico.socialsoftware.quizzes.command.GetStudentByExecutionIdAndUserIdCommand;
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.execution.service.CourseExecutionService;
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.user.aggregate.UserDto;

@Component
public class GetStudentByExecutionIdAndUserIdCommandHandler implements CommandHandler<GetStudentByExecutionIdAndUserIdCommand> {

    private final CourseExecutionService courseExecutionService;

    public GetStudentByExecutionIdAndUserIdCommandHandler(CourseExecutionService courseExecutionService) {
        this.courseExecutionService = courseExecutionService;
    }
    @Override
    public void handle(GetStudentByExecutionIdAndUserIdCommand command) {
        UserDto userDto = courseExecutionService.getStudentByExecutionIdAndUserId(
                command.getExecutionAggregateId(),
                command.getUserAggregateId(),
                command.getUnitOfWork()
        );
        command.setUserDto(userDto);
    }
}
