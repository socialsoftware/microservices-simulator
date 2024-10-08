package pt.ulisboa.tecnico.socialsoftware.ms.quizzes.sagas.coordination.workflows;

import pt.ulisboa.tecnico.socialsoftware.ms.coordination.workflow.WorkflowFunctionality;
import pt.ulisboa.tecnico.socialsoftware.ms.quizzes.microservices.user.aggregate.UserDto;
import pt.ulisboa.tecnico.socialsoftware.ms.quizzes.microservices.user.service.UserService;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.unityOfWork.SagaUnitOfWork;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.unityOfWork.SagaUnitOfWorkService;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.workflow.SagaSyncStep;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.workflow.SagaWorkflow;

public class CreateUserFunctionalitySagas extends WorkflowFunctionality {
    private UserDto userDto;
    private UserDto createdUserDto;
    private final UserService userService;
    private final SagaUnitOfWorkService unitOfWorkService;

    public CreateUserFunctionalitySagas(UserService userService, SagaUnitOfWorkService unitOfWorkService,  
                            UserDto userDto, SagaUnitOfWork unitOfWork) {
        this.userService = userService;
        this.unitOfWorkService = unitOfWorkService;
        this.buildWorkflow(userDto, unitOfWork);
    }

    public void buildWorkflow(UserDto userDto, SagaUnitOfWork unitOfWork) {
        this.workflow = new SagaWorkflow(this, unitOfWorkService, unitOfWork);

        SagaSyncStep createUserStep = new SagaSyncStep("createUserStep", () -> {
            UserDto createdUserDto = userService.createUser(userDto, unitOfWork);
            this.setCreatedUserDto(createdUserDto);
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