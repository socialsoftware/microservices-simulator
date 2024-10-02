package pt.ulisboa.tecnico.socialsoftware.ms.quizzes.sagas.coordination.workflows;

import java.util.ArrayList;
import java.util.Arrays;

import pt.ulisboa.tecnico.socialsoftware.ms.coordination.workflow.SyncStep;
import pt.ulisboa.tecnico.socialsoftware.ms.coordination.workflow.WorkflowFunctionality;
import pt.ulisboa.tecnico.socialsoftware.ms.quizzes.microservices.exception.ErrorMessage;
import pt.ulisboa.tecnico.socialsoftware.ms.quizzes.microservices.exception.TutorException;
import pt.ulisboa.tecnico.socialsoftware.ms.quizzes.microservices.user.service.UserService;
import pt.ulisboa.tecnico.socialsoftware.ms.quizzes.sagas.aggregates.SagaUser;
import pt.ulisboa.tecnico.socialsoftware.ms.quizzes.sagas.aggregates.dtos.SagaUserDto;
import pt.ulisboa.tecnico.socialsoftware.ms.quizzes.sagas.aggregates.states.TournamentSagaState;
import pt.ulisboa.tecnico.socialsoftware.ms.quizzes.sagas.aggregates.states.UserSagaState;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.aggregate.GenericSagaState;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.unityOfWork.SagaUnitOfWork;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.unityOfWork.SagaUnitOfWorkService;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.workflow.SagaSyncStep;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.workflow.SagaWorkflow;

import static pt.ulisboa.tecnico.socialsoftware.ms.quizzes.microservices.exception.ErrorMessage.AGGREGATE_BEING_USED_IN_OTHER_SAGA;

public class ActivateUserFunctionalitySagas extends WorkflowFunctionality {

    private SagaUserDto user;
    private final UserService userService;
    private final SagaUnitOfWorkService unitOfWorkService;

    public ActivateUserFunctionalitySagas(UserService userService, SagaUnitOfWorkService unitOfWorkService,  
                            Integer userAggregateId, SagaUnitOfWork unitOfWork) {
        this.userService = userService;
        this.unitOfWorkService = unitOfWorkService;
        this.buildWorkflow(userAggregateId, unitOfWork);
    }

    public void buildWorkflow(Integer userAggregateId, SagaUnitOfWork unitOfWork) {
        this.workflow = new SagaWorkflow(this, unitOfWorkService, unitOfWork);

        SagaSyncStep getUserStep = new SagaSyncStep("getUserStep", () -> {
            SagaUserDto user = (SagaUserDto) userService.getUserById(userAggregateId, unitOfWork);
            if (user.getSagaState().equals(GenericSagaState.NOT_IN_SAGA)) {
                unitOfWorkService.registerSagaState(userAggregateId, UserSagaState.READ_USER, unitOfWork);
                this.setUser(user);
            }
            else {
                throw new TutorException(ErrorMessage.AGGREGATE_BEING_USED_IN_OTHER_SAGA);
            }
        });
    
        getUserStep.registerCompensation(() -> {
            unitOfWorkService.registerSagaState(userAggregateId, GenericSagaState.NOT_IN_SAGA, unitOfWork);
        }, unitOfWork);
    
        SyncStep activateUserStep = new SyncStep("activateUserStep", () -> {
            userService.activateUser(userAggregateId, unitOfWork);
        }, new ArrayList<>(Arrays.asList(getUserStep)));
    
        workflow.addStep(getUserStep);
        workflow.addStep(activateUserStep);
    }

    @Override
    public void handleEvents() {

    }    

    public SagaUserDto getUser() {
        return user;
    }

    public void setUser(SagaUserDto user) {
        this.user = user;
    }
}