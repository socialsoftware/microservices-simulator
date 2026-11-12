package pt.ulisboa.tecnico.socialsoftware.quizzesfull2.microservices.user.coordination.sagas;

import pt.ulisboa.tecnico.socialsoftware.ms.coordination.WorkflowFunctionality;
import pt.ulisboa.tecnico.socialsoftware.ms.messaging.CommandGateway;
import pt.ulisboa.tecnico.socialsoftware.ms.transaction.sagas.unitOfWork.SagaUnitOfWork;
import pt.ulisboa.tecnico.socialsoftware.ms.transaction.sagas.unitOfWork.SagaUnitOfWorkService;
import pt.ulisboa.tecnico.socialsoftware.ms.transaction.sagas.workflow.SagaStep;
import pt.ulisboa.tecnico.socialsoftware.ms.transaction.sagas.workflow.SagaWorkflow;
import pt.ulisboa.tecnico.socialsoftware.quizzesfull2.ServiceMapping;
import pt.ulisboa.tecnico.socialsoftware.quizzesfull2.commands.user.CreateUserCommand;
import pt.ulisboa.tecnico.socialsoftware.quizzesfull2.microservices.user.aggregate.UserDto;

public class CreateUserFunctionalitySagas extends WorkflowFunctionality {
    private UserDto userDto;

    public CreateUserFunctionalitySagas(SagaUnitOfWorkService unitOfWorkService, UserDto userDto,
                                        SagaUnitOfWork unitOfWork, CommandGateway commandGateway) {
        buildWorkflow(unitOfWorkService, userDto, unitOfWork, commandGateway);
    }

    public void buildWorkflow(SagaUnitOfWorkService unitOfWorkService, UserDto userDto,
                              SagaUnitOfWork unitOfWork, CommandGateway commandGateway) {
        this.workflow = new SagaWorkflow(this, unitOfWorkService, unitOfWork);

        SagaStep createUserStep = new SagaStep("createUserStep", () -> {
            CreateUserCommand cmd = new CreateUserCommand(
                    unitOfWork, ServiceMapping.USER.getServiceName(), userDto);
            this.userDto = (UserDto) commandGateway.send(cmd);
        });

        this.workflow.addStep(createUserStep);
    }

    public UserDto getUserDto() {
        return userDto;
    }
}
