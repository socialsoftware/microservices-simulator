package pt.ulisboa.tecnico.socialsoftware.quizzesfull2.microservices.user.coordination.sagas;

import pt.ulisboa.tecnico.socialsoftware.ms.coordination.WorkflowFunctionality;
import pt.ulisboa.tecnico.socialsoftware.ms.messaging.CommandGateway;
import pt.ulisboa.tecnico.socialsoftware.ms.transaction.sagas.messaging.SagaCommand;
import pt.ulisboa.tecnico.socialsoftware.ms.transaction.sagas.unitOfWork.SagaUnitOfWork;
import pt.ulisboa.tecnico.socialsoftware.ms.transaction.sagas.unitOfWork.SagaUnitOfWorkService;
import pt.ulisboa.tecnico.socialsoftware.ms.transaction.sagas.workflow.SagaStep;
import pt.ulisboa.tecnico.socialsoftware.ms.transaction.sagas.workflow.SagaWorkflow;
import pt.ulisboa.tecnico.socialsoftware.quizzesfull2.ServiceMapping;
import pt.ulisboa.tecnico.socialsoftware.quizzesfull2.commands.user.AnonymizeUserCommand;
import pt.ulisboa.tecnico.socialsoftware.quizzesfull2.commands.user.GetUserByIdCommand;
import pt.ulisboa.tecnico.socialsoftware.quizzesfull2.microservices.user.aggregate.UserDto;
import pt.ulisboa.tecnico.socialsoftware.quizzesfull2.microservices.user.aggregate.sagas.states.UserSagaState;

import java.util.ArrayList;
import java.util.Arrays;

public class AnonymizeUserFunctionalitySagas extends WorkflowFunctionality {
    private UserDto userDto;

    public AnonymizeUserFunctionalitySagas(SagaUnitOfWorkService unitOfWorkService, Integer userAggregateId,
                                           SagaUnitOfWork unitOfWork, CommandGateway commandGateway) {
        buildWorkflow(unitOfWorkService, userAggregateId, unitOfWork, commandGateway);
    }

    public void buildWorkflow(SagaUnitOfWorkService unitOfWorkService, Integer userAggregateId,
                              SagaUnitOfWork unitOfWork, CommandGateway commandGateway) {
        this.workflow = new SagaWorkflow(this, unitOfWorkService, unitOfWork);

        SagaStep getUserStep = new SagaStep("getUserStep", () -> {
            GetUserByIdCommand readCmd = new GetUserByIdCommand(
                    unitOfWork, ServiceMapping.USER.getServiceName(), userAggregateId);
            SagaCommand sagaCommand = new SagaCommand(readCmd);
            sagaCommand.setSemanticLock(UserSagaState.IN_ANONYMIZE_USER);
            this.userDto = (UserDto) commandGateway.send(sagaCommand);
        });

        SagaStep anonymizeUserStep = new SagaStep("anonymizeUserStep", () -> {
            AnonymizeUserCommand cmd = new AnonymizeUserCommand(
                    unitOfWork, ServiceMapping.USER.getServiceName(), userAggregateId);
            commandGateway.send(cmd);
        }, new ArrayList<>(Arrays.asList(getUserStep)));

        this.workflow.addStep(getUserStep);
        this.workflow.addStep(anonymizeUserStep);
    }

    public UserDto getUserDto() {
        return userDto;
    }
}
