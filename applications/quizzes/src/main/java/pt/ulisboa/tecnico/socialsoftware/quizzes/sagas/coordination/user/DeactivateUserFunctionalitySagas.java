package pt.ulisboa.tecnico.socialsoftware.quizzes.sagas.coordination.user;

import java.util.ArrayList;
import java.util.Arrays;

import pt.ulisboa.tecnico.socialsoftware.ms.coordination.workflow.SyncStep;
import pt.ulisboa.tecnico.socialsoftware.ms.coordination.workflow.WorkflowFunctionality;
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.user.service.UserService;
import pt.ulisboa.tecnico.socialsoftware.quizzes.sagas.aggregates.dtos.SagaUserDto;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.unitOfWork.SagaUnitOfWork;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.unitOfWork.SagaUnitOfWorkService;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.workflow.SagaSyncStep;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.workflow.SagaWorkflow;

public class DeactivateUserFunctionalitySagas extends WorkflowFunctionality {

    private SagaUserDto user;
    private final UserService userService;
    private final SagaUnitOfWorkService unitOfWorkService;

    public DeactivateUserFunctionalitySagas(UserService userService, SagaUnitOfWorkService unitOfWorkService,
                                             Integer userAggregateId, SagaUnitOfWork unitOfWork) {
        this.userService = userService;
        this.unitOfWorkService = unitOfWorkService;
        this.buildWorkflow(userAggregateId, unitOfWork);
    }

    public void buildWorkflow(Integer userAggregateId, SagaUnitOfWork unitOfWork) {
        this.workflow = new SagaWorkflow(this, unitOfWorkService, unitOfWork);

        SagaSyncStep getUserStep = new SagaSyncStep("getUserStep", () -> {
            SagaUserDto user = (SagaUserDto) userService.getUserById(userAggregateId, unitOfWork);
                this.setUser(user);
        });

        SyncStep deactivateUserStep = new SyncStep("deactivateUserStep", () -> {
            userService.deactivateUser(userAggregateId, unitOfWork);
        }, new ArrayList<>(Arrays.asList(getUserStep)));
    
        workflow.addStep(getUserStep);
        workflow.addStep(deactivateUserStep);
    }

        

    public SagaUserDto getUser() {
        return user;
    }

    public void setUser(SagaUserDto user) {
        this.user = user;
    }
}