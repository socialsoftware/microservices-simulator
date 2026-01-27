package pt.ulisboa.tecnico.socialsoftware.answers.sagas.coordination.user;

import pt.ulisboa.tecnico.socialsoftware.ms.coordination.workflow.WorkflowFunctionality;
import pt.ulisboa.tecnico.socialsoftware.answers.microservices.user.service.UserService;
import pt.ulisboa.tecnico.socialsoftware.answers.shared.dtos.UserDto;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.unitOfWork.SagaUnitOfWork;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.unitOfWork.SagaUnitOfWorkService;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.workflow.SagaSyncStep;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.workflow.SagaWorkflow;
import pt.ulisboa.tecnico.socialsoftware.answers.coordination.webapi.requestDtos.CreateUserRequestDto;

public class CreateUserFunctionalitySagas extends WorkflowFunctionality {
    private UserDto createdUserDto;
    private final UserService userService;
    private final SagaUnitOfWorkService unitOfWorkService;


    public CreateUserFunctionalitySagas(SagaUnitOfWork unitOfWork, SagaUnitOfWorkService unitOfWorkService, UserService userService, CreateUserRequestDto createRequest) {
        this.userService = userService;
        this.unitOfWorkService = unitOfWorkService;
        this.buildWorkflow(createRequest, unitOfWork);
    }

    public void buildWorkflow(CreateUserRequestDto createRequest, SagaUnitOfWork unitOfWork) {
        this.workflow = new SagaWorkflow(this, unitOfWorkService, unitOfWork);

        SagaSyncStep createUserStep = new SagaSyncStep("createUserStep", () -> {
            UserDto createdUserDto = userService.createUser(createRequest, unitOfWork);
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
