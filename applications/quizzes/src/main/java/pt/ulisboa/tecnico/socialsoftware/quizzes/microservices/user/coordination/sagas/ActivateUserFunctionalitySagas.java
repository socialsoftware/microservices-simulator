package pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.user.coordination.sagas;

import pt.ulisboa.tecnico.socialsoftware.ms.coordination.workflow.CommandGateway;
import pt.ulisboa.tecnico.socialsoftware.ms.coordination.workflow.SyncStep;
import pt.ulisboa.tecnico.socialsoftware.ms.coordination.workflow.WorkflowFunctionality;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.unitOfWork.SagaUnitOfWork;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.unitOfWork.SagaUnitOfWorkService;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.workflow.SagaSyncStep;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.workflow.SagaWorkflow;
import pt.ulisboa.tecnico.socialsoftware.quizzes.ServiceMapping;
import pt.ulisboa.tecnico.socialsoftware.quizzes.command.user.ActivateUserCommand;
import pt.ulisboa.tecnico.socialsoftware.quizzes.command.user.GetUserByIdCommand;
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.user.aggregate.UserDto;

import java.util.ArrayList;
import java.util.Arrays;

public class ActivateUserFunctionalitySagas extends WorkflowFunctionality {
    private UserDto user;
    private final SagaUnitOfWorkService unitOfWorkService;
    private final CommandGateway commandGateway;

    public ActivateUserFunctionalitySagas(SagaUnitOfWorkService unitOfWorkService,
                                          Integer userAggregateId, SagaUnitOfWork unitOfWork, CommandGateway commandGateway) {
        this.unitOfWorkService = unitOfWorkService;
        this.commandGateway = commandGateway;
        this.buildWorkflow(userAggregateId, unitOfWork);
    }

    public void buildWorkflow(Integer userAggregateId, SagaUnitOfWork unitOfWork) {
        this.workflow = new SagaWorkflow(this, unitOfWorkService, unitOfWork);

        SagaSyncStep getUserStep = new SagaSyncStep("getUserStep", () -> {
            GetUserByIdCommand getUserByIdCommand = new GetUserByIdCommand(unitOfWork, ServiceMapping.USER.getServiceName(), userAggregateId);
            UserDto user = (UserDto) commandGateway.send(getUserByIdCommand);
            this.setUser(user);
        });

        SyncStep activateUserStep = new SyncStep("activateUserStep", () -> {
            ActivateUserCommand activateUserCommand = new ActivateUserCommand(unitOfWork, ServiceMapping.USER.getServiceName(), userAggregateId);
            commandGateway.send(activateUserCommand);
        }, new ArrayList<>(Arrays.asList(getUserStep)));

        workflow.addStep(getUserStep);
        workflow.addStep(activateUserStep);
    }

    public UserDto getUser() {
        return user;
    }

    public void setUser(UserDto user) {
        this.user = user;
    }
}