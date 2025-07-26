package pt.ulisboa.tecnico.socialsoftware.quizzes.sagas.coordination.user;

import pt.ulisboa.tecnico.socialsoftware.ms.coordination.workflow.WorkflowFunctionality;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.aggregate.GenericSagaState;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.unitOfWork.SagaUnitOfWork;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.unitOfWork.SagaUnitOfWorkService;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.workflow.SagaSyncStep;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.workflow.SagaWorkflow;
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.user.service.UserService;
import pt.ulisboa.tecnico.socialsoftware.quizzes.sagas.aggregates.dtos.SagaUserDto;
import pt.ulisboa.tecnico.socialsoftware.quizzes.sagas.aggregates.states.UserSagaState;

import java.util.ArrayList;
import java.util.Arrays;

public class DeleteUserFunctionalitySagas extends WorkflowFunctionality {

    private SagaUserDto user;
    private final UserService userService;
    private final SagaUnitOfWorkService unitOfWorkService;

    public DeleteUserFunctionalitySagas(UserService userService, SagaUnitOfWorkService unitOfWorkService,  
                            Integer userAggregateId, SagaUnitOfWork unitOfWork) {
        this.userService = userService;
        this.unitOfWorkService = unitOfWorkService;
        this.buildWorkflow(userAggregateId, unitOfWork);
    }

    public void buildWorkflow(Integer userAggregateId, SagaUnitOfWork unitOfWork) {
        this.workflow = new SagaWorkflow(this, unitOfWorkService, unitOfWork);

        SagaSyncStep getUserStep = new SagaSyncStep("getUserStep", () -> {
            SagaUserDto user = (SagaUserDto) userService.getUserById(userAggregateId, unitOfWork);
            unitOfWorkService.registerSagaState(userAggregateId, UserSagaState.READ_USER, unitOfWork);
            this.setUser(user);
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
    public SagaUserDto getUser() {
        return user;
    }

    public void setUser(SagaUserDto user) {
        this.user = user;
    }
}