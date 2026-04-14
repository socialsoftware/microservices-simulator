package pt.ulisboa.tecnico.socialsoftware.ecommerce.microservices.payment.coordination.sagas;

import pt.ulisboa.tecnico.socialsoftware.ms.coordination.workflow.WorkflowFunctionality;
import pt.ulisboa.tecnico.socialsoftware.ms.coordination.workflow.command.CommandGateway;
import pt.ulisboa.tecnico.socialsoftware.ecommerce.ServiceMapping;
import pt.ulisboa.tecnico.socialsoftware.ecommerce.command.payment.*;
import pt.ulisboa.tecnico.socialsoftware.ecommerce.shared.dtos.PaymentDto;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.unitOfWork.SagaUnitOfWork;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.unitOfWork.SagaUnitOfWorkService;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.workflow.SagaStep;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.workflow.SagaWorkflow;

public class GetPaymentByIdFunctionalitySagas extends WorkflowFunctionality {
    private PaymentDto paymentDto;
    private final SagaUnitOfWorkService unitOfWorkService;
    private final CommandGateway commandGateway;


    public GetPaymentByIdFunctionalitySagas(SagaUnitOfWorkService unitOfWorkService, Integer paymentAggregateId, SagaUnitOfWork unitOfWork, CommandGateway commandGateway) {
        this.unitOfWorkService = unitOfWorkService;
        this.commandGateway = commandGateway;
        this.buildWorkflow(paymentAggregateId, unitOfWork);
    }

    public void buildWorkflow(Integer paymentAggregateId, SagaUnitOfWork unitOfWork) {
        this.workflow = new SagaWorkflow(this, unitOfWorkService, unitOfWork);

        SagaStep getPaymentStep = new SagaStep("getPaymentStep", () -> {
            GetPaymentByIdCommand cmd = new GetPaymentByIdCommand(unitOfWork, ServiceMapping.PAYMENT.getServiceName(), paymentAggregateId);
            PaymentDto paymentDto = (PaymentDto) commandGateway.send(cmd);
            setPaymentDto(paymentDto);
        });

        workflow.addStep(getPaymentStep);
    }
    public PaymentDto getPaymentDto() {
        return paymentDto;
    }

    public void setPaymentDto(PaymentDto paymentDto) {
        this.paymentDto = paymentDto;
    }
}
