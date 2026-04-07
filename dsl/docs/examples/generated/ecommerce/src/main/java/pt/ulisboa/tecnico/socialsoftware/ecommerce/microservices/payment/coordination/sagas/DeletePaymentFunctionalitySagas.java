package pt.ulisboa.tecnico.socialsoftware.ecommerce.microservices.payment.coordination.sagas;

import pt.ulisboa.tecnico.socialsoftware.ms.coordination.workflow.WorkflowFunctionality;
import pt.ulisboa.tecnico.socialsoftware.ms.coordination.workflow.command.CommandGateway;
import pt.ulisboa.tecnico.socialsoftware.ecommerce.ServiceMapping;
import pt.ulisboa.tecnico.socialsoftware.ecommerce.command.payment.*;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.unitOfWork.SagaUnitOfWork;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.unitOfWork.SagaUnitOfWorkService;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.workflow.SagaStep;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.workflow.SagaWorkflow;

public class DeletePaymentFunctionalitySagas extends WorkflowFunctionality {
    private final SagaUnitOfWorkService unitOfWorkService;
    private final CommandGateway commandGateway;


    public DeletePaymentFunctionalitySagas(SagaUnitOfWorkService unitOfWorkService, Integer paymentAggregateId, SagaUnitOfWork unitOfWork, CommandGateway commandGateway) {
        this.unitOfWorkService = unitOfWorkService;
        this.commandGateway = commandGateway;
        this.buildWorkflow(paymentAggregateId, unitOfWork);
    }

    public void buildWorkflow(Integer paymentAggregateId, SagaUnitOfWork unitOfWork) {
        this.workflow = new SagaWorkflow(this, unitOfWorkService, unitOfWork);

        SagaStep deletePaymentStep = new SagaStep("deletePaymentStep", () -> {
            DeletePaymentCommand cmd = new DeletePaymentCommand(unitOfWork, ServiceMapping.PAYMENT.getServiceName(), paymentAggregateId);
            commandGateway.send(cmd);
        });

        workflow.addStep(deletePaymentStep);
    }
}
