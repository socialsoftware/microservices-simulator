package pt.ulisboa.tecnico.socialsoftware.answers.sagas.coordination.user;

import pt.ulisboa.tecnico.socialsoftware.ms.coordination.workflow.WorkflowFunctionality;
import pt.ulisboa.tecnico.socialsoftware.answers.microservices.user.service.UserService;
import pt.ulisboa.tecnico.socialsoftware.answers.shared.dtos.UserDto;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.unitOfWork.SagaUnitOfWork;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.unitOfWork.SagaUnitOfWorkService;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.workflow.SagaSyncStep;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.workflow.SagaWorkflow;
import java.util.ArrayList;
import java.util.Arrays;
import pt.ulisboa.tecnico.socialsoftware.answers.sagas.aggregates.states.UserSagaState;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.aggregate.GenericSagaState;

public class UpdateUserFunctionalitySagas extends WorkflowFunctionality {
    private UserDto updatedUserDto;
    private final UserService userService;
    private final SagaUnitOfWorkService unitOfWorkService;

    public UpdateUserFunctionalitySagas(UserService userService, SagaUnitOfWorkService unitOfWorkService, Integer userAggregateId, UserDto userDto, SagaUnitOfWork unitOfWork) {
        this.userService = userService;
        this.unitOfWorkService = unitOfWorkService;
        this.buildWorkflow(userAggregateId, userDto, unitOfWork);
    }

    public void buildWorkflow(Integer userAggregateId, UserDto userDto, SagaUnitOfWork unitOfWork) {
        this.workflow = new SagaWorkflow(this, unitOfWorkService, unitOfWork);

        SagaSyncStep getUserStep = new SagaSyncStep("getUserStep", () -> {
            unitOfWorkService.registerSagaState(userAggregateId, UserSagaState.READ_USER, unitOfWork);
        });

        getUserStep.registerCompensation(() -> {
            unitOfWorkService.registerSagaState(userAggregateId, GenericSagaState.NOT_IN_SAGA, unitOfWork);
        }, unitOfWork);

        SagaSyncStep updateUserStep = new SagaSyncStep("updateUserStep", () -> {
            UserDto updatedUserDto = userService.updateUser(userAggregateId, userDto, unitOfWork);
            setUpdatedUserDto(updatedUserDto);
        }, new ArrayList<>(Arrays.asList(getUserStep)));

        workflow.addStep(getUserStep);
        workflow.addStep(updateUserStep);
    }

    public UserDto getUpdatedUserDto() {
        return updatedUserDto;
    }

    public void setUpdatedUserDto(UserDto updatedUserDto) {
        this.updatedUserDto = updatedUserDto;
    }
}
