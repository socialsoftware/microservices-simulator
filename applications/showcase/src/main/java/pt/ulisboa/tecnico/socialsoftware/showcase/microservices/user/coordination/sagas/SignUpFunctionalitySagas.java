package pt.ulisboa.tecnico.socialsoftware.showcase.microservices.user.coordination.sagas;

import pt.ulisboa.tecnico.socialsoftware.ms.coordination.workflow.WorkflowFunctionality;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.workflow.SagaWorkflow;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.workflow.SagaStep;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.unitOfWork.SagaUnitOfWork;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.unitOfWork.SagaUnitOfWorkService;
import pt.ulisboa.tecnico.socialsoftware.ms.coordination.workflow.command.CommandGateway;
import pt.ulisboa.tecnico.socialsoftware.showcase.ServiceMapping;
import pt.ulisboa.tecnico.socialsoftware.showcase.command.user.*;
import pt.ulisboa.tecnico.socialsoftware.showcase.shared.dtos.UserDto;
import pt.ulisboa.tecnico.socialsoftware.showcase.microservices.user.service.UserService;

public class SignUpFunctionalitySagas extends WorkflowFunctionality {
    
        private final SagaUnitOfWorkService sagaUnitOfWorkService;
    private final UserService userService;
    private final SagaUnitOfWork unitOfWork;
    private final CommandGateway commandGateway;

    public SignUpFunctionalitySagas(SagaUnitOfWorkService sagaUnitOfWorkService, UserService userService, String username, String email, SagaUnitOfWork unitOfWork, CommandGateway commandGateway) {
        this.sagaUnitOfWorkService = sagaUnitOfWorkService;
        this.userService = userService;
        this.unitOfWork = unitOfWork;
        this.commandGateway = commandGateway;
        this.buildWorkflow(username, email);
    }

    public void buildWorkflow(String username, String email) {
        this.workflow = new SagaWorkflow(this, this.sagaUnitOfWorkService, this.unitOfWork);
        SagaStep signUpStep = new SagaStep("signUpStep", () -> {
            this.userService.signUp(username, email, this.unitOfWork);
        });
        this.workflow.addStep(signUpStep);
    }

}
