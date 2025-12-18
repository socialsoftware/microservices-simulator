package pt.ulisboa.tecnico.socialsoftware.answers.sagas.coordination.user;

import pt.ulisboa.tecnico.socialsoftware.ms.coordination.workflow.WorkflowFunctionality;
import pt.ulisboa.tecnico.socialsoftware.answers.microservices.user.service.UserService;
import pt.ulisboa.tecnico.socialsoftware.answers.shared.dtos.UserDto;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.unitOfWork.SagaUnitOfWork;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.unitOfWork.SagaUnitOfWorkService;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.workflow.SagaSyncStep;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.workflow.SagaWorkflow;

public class UpdateUserFunctionalitySagas extends WorkflowFunctionality {
    private UserDto updatedUserDto;
    private final UserService userService;
    private final SagaUnitOfWorkService unitOfWorkService;


    public UpdateUserFunctionalitySagas(SagaUnitOfWork unitOfWork, SagaUnitOfWorkService unitOfWorkService, UserService userService, Integer userAggregateId, UserDto userDto) {
        this.userService = userService;
        this.unitOfWorkService = unitOfWorkService;
        this.buildWorkflow(userAggregateId, userDto, unitOfWork);
    }

    public void buildWorkflow(Integer userAggregateId, UserDto userDto, SagaUnitOfWork unitOfWork) {
        this.workflow = new SagaWorkflow(this, unitOfWorkService, unitOfWork);

        SagaSyncStep updateUserStep = new SagaSyncStep("updateUserStep", () -> {
            UserDto updatedUserDto = userService.updateUser(userAggregateId, userDto, unitOfWork);
            setUpdatedUserDto(updatedUserDto);
        });

        workflow.addStep(updateUserStep);

    }

    public UserDto getUpdatedUserDto() {
        return updatedUserDto;
    }

    public void setUpdatedUserDto(UserDto updatedUserDto) {
        this.updatedUserDto = updatedUserDto;
    }
}
