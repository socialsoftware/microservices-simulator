package pt.ulisboa.tecnico.socialsoftware.quizzesfull2.microservices.user.coordination.sagas;

import pt.ulisboa.tecnico.socialsoftware.ms.coordination.WorkflowFunctionality;
import pt.ulisboa.tecnico.socialsoftware.ms.messaging.CommandGateway;
import pt.ulisboa.tecnico.socialsoftware.ms.transaction.sagas.unitOfWork.SagaUnitOfWork;
import pt.ulisboa.tecnico.socialsoftware.ms.transaction.sagas.unitOfWork.SagaUnitOfWorkService;
import pt.ulisboa.tecnico.socialsoftware.ms.transaction.sagas.workflow.SagaStep;
import pt.ulisboa.tecnico.socialsoftware.ms.transaction.sagas.workflow.SagaWorkflow;
import pt.ulisboa.tecnico.socialsoftware.quizzesfull2.ServiceMapping;
import pt.ulisboa.tecnico.socialsoftware.quizzesfull2.commands.user.GetUserByIdCommand;
import pt.ulisboa.tecnico.socialsoftware.quizzesfull2.microservices.user.aggregate.UserDto;

public class GetUserByIdFunctionalitySagas extends WorkflowFunctionality {
    private UserDto userDto;

    public GetUserByIdFunctionalitySagas(SagaUnitOfWorkService unitOfWorkService, Integer userAggregateId,
                                         SagaUnitOfWork unitOfWork, CommandGateway commandGateway) {
        buildWorkflow(unitOfWorkService, userAggregateId, unitOfWork, commandGateway);
    }

    public void buildWorkflow(SagaUnitOfWorkService unitOfWorkService, Integer userAggregateId,
                              SagaUnitOfWork unitOfWork, CommandGateway commandGateway) {
        this.workflow = new SagaWorkflow(this, unitOfWorkService, unitOfWork);

        SagaStep getUserStep = new SagaStep("getUserStep", () -> {
            GetUserByIdCommand cmd = new GetUserByIdCommand(
                    unitOfWork, ServiceMapping.USER.getServiceName(), userAggregateId);
            this.userDto = (UserDto) commandGateway.send(cmd);
        });

        this.workflow.addStep(getUserStep);
    }

    public UserDto getUserDto() {
        return userDto;
    }
}
