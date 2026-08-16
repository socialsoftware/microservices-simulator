package pt.ulisboa.tecnico.socialsoftware.quizzesfull.microservices.user.coordination.sagas;

import pt.ulisboa.tecnico.socialsoftware.ms.coordination.WorkflowFunctionality;
import pt.ulisboa.tecnico.socialsoftware.ms.messaging.CommandGateway;
import pt.ulisboa.tecnico.socialsoftware.ms.transaction.sagas.unitOfWork.SagaUnitOfWork;
import pt.ulisboa.tecnico.socialsoftware.ms.transaction.sagas.unitOfWork.SagaUnitOfWorkService;
import pt.ulisboa.tecnico.socialsoftware.ms.transaction.sagas.workflow.SagaStep;
import pt.ulisboa.tecnico.socialsoftware.ms.transaction.sagas.workflow.SagaWorkflow;
import pt.ulisboa.tecnico.socialsoftware.quizzesfull.ServiceMapping;
import pt.ulisboa.tecnico.socialsoftware.quizzesfull.commands.user.CreateUserCommand;
import pt.ulisboa.tecnico.socialsoftware.quizzesfull.microservices.user.aggregate.UserDto;

public class CreateUserFunctionalitySagas extends WorkflowFunctionality {
    private UserDto createdUserDto;
    private final SagaUnitOfWorkService unitOfWorkService;
    private final CommandGateway commandGateway;

    public CreateUserFunctionalitySagas(SagaUnitOfWorkService unitOfWorkService,
                                        UserDto userDto,
                                        SagaUnitOfWork unitOfWork, CommandGateway commandGateway) {
        this.unitOfWorkService = unitOfWorkService;
        this.commandGateway = commandGateway;
        this.buildWorkflow(userDto, unitOfWork);
    }

    public void buildWorkflow(UserDto userDto, SagaUnitOfWork unitOfWork) {
        this.workflow = new SagaWorkflow(this, unitOfWorkService, unitOfWork);

        SagaStep createUserStep = new SagaStep("createUserStep", () -> {
            CreateUserCommand cmd = new CreateUserCommand(
                    unitOfWork, ServiceMapping.USER.getServiceName(), userDto);
            this.createdUserDto = (UserDto) commandGateway.send(cmd);
        });

        workflow.addStep(createUserStep);
    }

    public UserDto getCreatedUserDto() {
        return createdUserDto;
    }
}
