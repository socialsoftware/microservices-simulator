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

public class AwardLoyaltyPointsFunctionalitySagas extends WorkflowFunctionality {
    
        private final SagaUnitOfWorkService sagaUnitOfWorkService;
    private final UserService userService;
    private final SagaUnitOfWork unitOfWork;
    private final CommandGateway commandGateway;

    public AwardLoyaltyPointsFunctionalitySagas(SagaUnitOfWorkService sagaUnitOfWorkService, UserService userService, Integer userId, Integer points, SagaUnitOfWork unitOfWork, CommandGateway commandGateway) {
        this.sagaUnitOfWorkService = sagaUnitOfWorkService;
        this.userService = userService;
        this.unitOfWork = unitOfWork;
        this.commandGateway = commandGateway;
        this.buildWorkflow(userId, points);
    }

    public void buildWorkflow(Integer userId, Integer points) {
        this.workflow = new SagaWorkflow(this, this.sagaUnitOfWorkService, this.unitOfWork);
        SagaStep awardLoyaltyPointsStep = new SagaStep("awardLoyaltyPointsStep", () -> {
            this.userService.awardLoyaltyPoints(userId, points, this.unitOfWork);
        });
        this.workflow.addStep(awardLoyaltyPointsStep);
    }

}
