package pt.ulisboa.tecnico.socialsoftware.quizzes.sagas.coordination.user;

import pt.ulisboa.tecnico.socialsoftware.ms.coordination.workflow.Command;
import pt.ulisboa.tecnico.socialsoftware.ms.coordination.workflow.CommandGateway;
import pt.ulisboa.tecnico.socialsoftware.ms.coordination.workflow.WorkflowFunctionality;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.aggregate.GenericSagaState;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.unitOfWork.SagaUnitOfWork;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.unitOfWork.SagaUnitOfWorkService;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.workflow.SagaSyncStep;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.workflow.SagaWorkflow;
import pt.ulisboa.tecnico.socialsoftware.quizzes.ServiceMapping;
import pt.ulisboa.tecnico.socialsoftware.quizzes.command.user.DeleteUserCommand;
import pt.ulisboa.tecnico.socialsoftware.quizzes.command.user.GetUserByIdCommand;
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.user.aggregate.UserDto;
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.user.service.UserService;
import pt.ulisboa.tecnico.socialsoftware.quizzes.sagas.aggregates.states.UserSagaState;

import java.util.ArrayList;
import java.util.Arrays;

public class DeleteUserFunctionalitySagas extends WorkflowFunctionality {

    private UserDto user;
    private final UserService userService;
    private final SagaUnitOfWorkService unitOfWorkService;
    private final CommandGateway commandGateway;

    public DeleteUserFunctionalitySagas(UserService userService, SagaUnitOfWorkService unitOfWorkService,
                            Integer userAggregateId, SagaUnitOfWork unitOfWork, CommandGateway commandGateway) {
        this.userService = userService;
        this.unitOfWorkService = unitOfWorkService;
        this.commandGateway = commandGateway;
        this.buildWorkflow(userAggregateId, unitOfWork);
    }

    public void buildWorkflow(Integer userAggregateId, SagaUnitOfWork unitOfWork) {
        this.workflow = new SagaWorkflow(this, unitOfWorkService, unitOfWork);

        SagaSyncStep getUserStep = new SagaSyncStep("getUserStep", () -> {
//            UserDto user = (UserDto) userService.getUserById(userAggregateId, unitOfWork);
//            unitOfWorkService.registerSagaState(userAggregateId, UserSagaState.READ_USER, unitOfWork);
            GetUserByIdCommand getUserByIdCommand = new GetUserByIdCommand(unitOfWork, ServiceMapping.USER.getServiceName(), userAggregateId);
            getUserByIdCommand.setSemanticLock(UserSagaState.READ_USER);
            UserDto user = (UserDto) commandGateway.send(getUserByIdCommand);
            this.setUser(user);
        });
    
        getUserStep.registerCompensation(() -> {
//            unitOfWorkService.registerSagaState(userAggregateId, GenericSagaState.NOT_IN_SAGA, unitOfWork);
            Command command = new Command(unitOfWork, ServiceMapping.USER.getServiceName(), userAggregateId);
            command.setSemanticLock(GenericSagaState.NOT_IN_SAGA);
            commandGateway.send(command);
        }, unitOfWork);
    
        SagaSyncStep deleteUserStep = new SagaSyncStep("deleteUserStep", () -> {
//            userService.deleteUser(userAggregateId, unitOfWork);
            DeleteUserCommand deleteUserCommand = new DeleteUserCommand(unitOfWork, ServiceMapping.USER.getServiceName(), userAggregateId);
            commandGateway.send(deleteUserCommand);
        }, new ArrayList<>(Arrays.asList(getUserStep)));
    
        workflow.addStep(getUserStep);
        workflow.addStep(deleteUserStep);
    }
    public UserDto getUser() {
        return user;
    }

    public void setUser(UserDto user) {
        this.user = user;
    }
}