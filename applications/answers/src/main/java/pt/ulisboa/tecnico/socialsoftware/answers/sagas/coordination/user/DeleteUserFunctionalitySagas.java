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

public class DeleteUserFunctionalitySagas extends WorkflowFunctionality {
    private UserDto deletedUserDto;
    private final UserService userService;
    private final SagaUnitOfWorkService unitOfWorkService;

    public DeleteUserFunctionalitySagas(UserService userService, SagaUnitOfWorkService unitOfWorkService, Integer userAggregateId, SagaUnitOfWork unitOfWork) {
        this.userService = userService;
        this.unitOfWorkService = unitOfWorkService;
        this.buildWorkflow(userAggregateId, unitOfWork);
    }

    public void buildWorkflow(Integer userAggregateId, SagaUnitOfWork unitOfWork) {
        this.workflow = new SagaWorkflow(this, unitOfWorkService, unitOfWork);

        SagaSyncStep getUserStep = new SagaSyncStep("getUserStep", () -> {
            UserDto deletedUserDto = userService.getUserById(userAggregateId, unitOfWork);
            setDeletedUserDto(deletedUserDto);
            unitOfWorkService.registerSagaState(userAggregateId, UserSagaState.READ_USER, unitOfWork);
        });

        getUserStep.registerCompensation(() -> {
            unitOfWorkService.registerSagaState(userAggregateId, GenericSagaState.NOT_IN_SAGA, unitOfWork);
        }, unitOfWork);

        SagaSyncStep deleteUserStep = new SagaSyncStep("deleteUserStep", () -> {
            userService.deleteUser(userAggregateId, unitOfWork);
        }, new ArrayList<>(Arrays.asList(getUserStep)));

        workflow.addStep(getUserStep);
        workflow.addStep(deleteUserStep);
    }

    public UserDto getDeletedUserDto() {
        return deletedUserDto;
    }

    public void setDeletedUserDto(UserDto deletedUserDto) {
        this.deletedUserDto = deletedUserDto;
    }
}
