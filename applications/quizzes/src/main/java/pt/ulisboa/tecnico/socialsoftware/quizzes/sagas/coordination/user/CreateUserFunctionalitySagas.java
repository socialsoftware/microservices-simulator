package pt.ulisboa.tecnico.socialsoftware.quizzes.sagas.coordination.user;

import pt.ulisboa.tecnico.socialsoftware.ms.coordination.workflow.CommandGateway;
import pt.ulisboa.tecnico.socialsoftware.ms.coordination.workflow.WorkflowFunctionality;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.unitOfWork.SagaUnitOfWork;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.unitOfWork.SagaUnitOfWorkService;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.workflow.SagaSyncStep;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.workflow.SagaWorkflow;
import pt.ulisboa.tecnico.socialsoftware.quizzes.ServiceMapping;
import pt.ulisboa.tecnico.socialsoftware.quizzes.command.user.CreateUserCommand;
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.user.aggregate.UserDto;
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.user.service.UserService;

public class CreateUserFunctionalitySagas extends WorkflowFunctionality {
    private UserDto userDto;
    private UserDto createdUserDto;
    private final UserService userService;
    private final SagaUnitOfWorkService unitOfWorkService;
    private final CommandGateway CommandGateway;

    public CreateUserFunctionalitySagas(UserService userService, SagaUnitOfWorkService unitOfWorkService,
            UserDto userDto, SagaUnitOfWork unitOfWork, CommandGateway CommandGateway) {
        this.userService = userService;
        this.unitOfWorkService = unitOfWorkService;
        this.CommandGateway = CommandGateway;
        this.buildWorkflow(userDto, unitOfWork);
    }

    public void buildWorkflow(UserDto userDto, SagaUnitOfWork unitOfWork) {
        this.workflow = new SagaWorkflow(this, unitOfWorkService, unitOfWork);

        SagaSyncStep createUserStep = new SagaSyncStep("createUserStep", () -> {
            // UserDto createdUserDto = userService.createUser(userDto, unitOfWork);
            CreateUserCommand createUserCommand = new CreateUserCommand(unitOfWork,
                    ServiceMapping.USER.getServiceName(), userDto);
            createdUserDto = (UserDto) CommandGateway.send(createUserCommand);
            setCreatedUserDto(createdUserDto);
        });

        workflow.addStep(createUserStep);
    }

    public UserDto getUserDto() {
        return userDto;
    }

    public void setUserDto(UserDto userDto) {
        this.userDto = userDto;
    }

    public UserDto getCreatedUserDto() {
        return createdUserDto;
    }

    public void setCreatedUserDto(UserDto createdUserDto) {
        this.createdUserDto = createdUserDto;
    }
}