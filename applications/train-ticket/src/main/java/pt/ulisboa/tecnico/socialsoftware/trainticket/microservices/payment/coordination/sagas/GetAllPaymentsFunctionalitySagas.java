package pt.ulisboa.tecnico.socialsoftware.trainticket.microservices.payment.coordination.sagas;

import pt.ulisboa.tecnico.socialsoftware.ms.coordination.WorkflowFunctionality;
import pt.ulisboa.tecnico.socialsoftware.ms.messaging.CommandGateway;
import pt.ulisboa.tecnico.socialsoftware.trainticket.ServiceMapping;
import pt.ulisboa.tecnico.socialsoftware.trainticket.command.payment.*;
import pt.ulisboa.tecnico.socialsoftware.trainticket.shared.dtos.PaymentDto;
import pt.ulisboa.tecnico.socialsoftware.ms.transaction.sagas.unitOfWork.SagaUnitOfWork;
import pt.ulisboa.tecnico.socialsoftware.ms.transaction.sagas.unitOfWork.SagaUnitOfWorkService;
import pt.ulisboa.tecnico.socialsoftware.ms.transaction.sagas.workflow.SagaStep;
import pt.ulisboa.tecnico.socialsoftware.ms.transaction.sagas.workflow.SagaWorkflow;
import java.util.List;

public class GetAllPaymentsFunctionalitySagas extends WorkflowFunctionality {
    private List<PaymentDto> payments;
    private final SagaUnitOfWorkService unitOfWorkService;
    private final CommandGateway commandGateway;


    public GetAllPaymentsFunctionalitySagas(SagaUnitOfWorkService unitOfWorkService, SagaUnitOfWork unitOfWork, CommandGateway commandGateway) {
        this.unitOfWorkService = unitOfWorkService;
        this.commandGateway = commandGateway;
        this.buildWorkflow(unitOfWork);
    }

    public void buildWorkflow(SagaUnitOfWork unitOfWork) {
        this.workflow = new SagaWorkflow(this, unitOfWorkService, unitOfWork);

        SagaStep getAllPaymentsStep = new SagaStep("getAllPaymentsStep", () -> {
            GetAllPaymentsCommand cmd = new GetAllPaymentsCommand(unitOfWork, ServiceMapping.PAYMENT.getServiceName());
            List<PaymentDto> payments = (List<PaymentDto>) commandGateway.send(cmd);
            setPayments(payments);
        });

        workflow.addStep(getAllPaymentsStep);
    }
    public List<PaymentDto> getPayments() {
        return payments;
    }

    public void setPayments(List<PaymentDto> payments) {
        this.payments = payments;
    }
}
