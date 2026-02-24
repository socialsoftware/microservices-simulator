package pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.user.coordination.causal;

import pt.ulisboa.tecnico.socialsoftware.ms.causal.unitOfWork.CausalUnitOfWork;
import pt.ulisboa.tecnico.socialsoftware.ms.causal.unitOfWork.CausalUnitOfWorkService;
import pt.ulisboa.tecnico.socialsoftware.ms.causal.workflow.CausalWorkflow;
import pt.ulisboa.tecnico.socialsoftware.ms.coordination.workflow.CommandGateway;
import pt.ulisboa.tecnico.socialsoftware.ms.coordination.workflow.Step;
import pt.ulisboa.tecnico.socialsoftware.ms.coordination.workflow.WorkflowFunctionality;
import pt.ulisboa.tecnico.socialsoftware.quizzes.ServiceMapping;
import pt.ulisboa.tecnico.socialsoftware.quizzes.command.user.CreateUserCommand;
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.user.aggregate.UserDto;

public class CreateUserFunctionalityTCC extends WorkflowFunctionality {
    private UserDto userDto;
    private UserDto createdUserDto;
    private final CausalUnitOfWorkService unitOfWorkService;
    private final CommandGateway commandGateway;

    public CreateUserFunctionalityTCC(CausalUnitOfWorkService unitOfWorkService,
                                      UserDto userDto, CausalUnitOfWork unitOfWork, CommandGateway commandGateway) {
        this.unitOfWorkService = unitOfWorkService;
        this.commandGateway = commandGateway;
        this.buildWorkflow(userDto, unitOfWork);
    }

    public void buildWorkflow(UserDto userDto, CausalUnitOfWork unitOfWork) {
        this.workflow = new CausalWorkflow(this, unitOfWorkService, unitOfWork);

        Step step = new Step(() -> {
            CreateUserCommand CreateUserCommand = new CreateUserCommand(unitOfWork, ServiceMapping.USER.getServiceName(), userDto);
            this.createdUserDto = (UserDto) commandGateway.send(CreateUserCommand);
        });

        workflow.addStep(step);
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