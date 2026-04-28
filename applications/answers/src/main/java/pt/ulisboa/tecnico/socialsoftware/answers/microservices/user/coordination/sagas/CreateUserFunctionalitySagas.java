package pt.ulisboa.tecnico.socialsoftware.answers.microservices.user.coordination.sagas;

import pt.ulisboa.tecnico.socialsoftware.ms.coordination.workflow.WorkflowFunctionality;
import pt.ulisboa.tecnico.socialsoftware.ms.coordination.workflow.command.CommandGateway;
import pt.ulisboa.tecnico.socialsoftware.answers.ServiceMapping;
import pt.ulisboa.tecnico.socialsoftware.answers.command.user.*;
import pt.ulisboa.tecnico.socialsoftware.answers.shared.dtos.UserDto;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.aggregate.SagaAggregate.SagaState;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.unitOfWork.SagaUnitOfWork;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.unitOfWork.SagaUnitOfWorkService;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.workflow.SagaStep;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.workflow.SagaWorkflow;
import pt.ulisboa.tecnico.socialsoftware.answers.microservices.user.aggregate.sagas.states.UserSagaState;
import pt.ulisboa.tecnico.socialsoftware.answers.microservices.user.coordination.webapi.requestDtos.CreateUserRequestDto;

public class CreateUserFunctionalitySagas extends WorkflowFunctionality {
    private UserDto createdUserDto;
    private final SagaUnitOfWorkService unitOfWorkService;
    private final CommandGateway commandGateway;


    public CreateUserFunctionalitySagas(SagaUnitOfWorkService unitOfWorkService, CreateUserRequestDto createRequest, SagaUnitOfWork unitOfWork, CommandGateway commandGateway) {
        this.unitOfWorkService = unitOfWorkService;
        this.commandGateway = commandGateway;
        this.buildWorkflow(createRequest, unitOfWork);
    }

    public void buildWorkflow(CreateUserRequestDto createRequest, SagaUnitOfWork unitOfWork) {
        this.workflow = new SagaWorkflow(this, unitOfWorkService, unitOfWork);

        SagaStep createUserStep = new SagaStep("createUserStep", () -> {
            CreateUserCommand cmd = new CreateUserCommand(unitOfWork, ServiceMapping.USER.getServiceName(), createRequest);
            UserDto createdUserDto = (UserDto) commandGateway.send(cmd);
            setCreatedUserDto(createdUserDto);
        });

        workflow.addStep(createUserStep);
    }
    public UserDto getCreatedUserDto() {
        return createdUserDto;
    }

    public void setCreatedUserDto(UserDto createdUserDto) {
        this.createdUserDto = createdUserDto;
    }
}
