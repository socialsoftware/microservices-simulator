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

public class UpdatePaymentFunctionalitySagas extends WorkflowFunctionality {
    private PaymentDto updatedPaymentDto;
    private final SagaUnitOfWorkService unitOfWorkService;
    private final CommandGateway commandGateway;


    public UpdatePaymentFunctionalitySagas(SagaUnitOfWorkService unitOfWorkService, PaymentDto paymentDto, SagaUnitOfWork unitOfWork, CommandGateway commandGateway) {
        this.unitOfWorkService = unitOfWorkService;
        this.commandGateway = commandGateway;
        this.buildWorkflow(paymentDto, unitOfWork);
    }

    public void buildWorkflow(PaymentDto paymentDto, SagaUnitOfWork unitOfWork) {
        this.workflow = new SagaWorkflow(this, unitOfWorkService, unitOfWork);

        SagaStep updatePaymentStep = new SagaStep("updatePaymentStep", () -> {
            UpdatePaymentCommand cmd = new UpdatePaymentCommand(unitOfWork, ServiceMapping.PAYMENT.getServiceName(), paymentDto);
            PaymentDto updatedPaymentDto = (PaymentDto) commandGateway.send(cmd);
            setUpdatedPaymentDto(updatedPaymentDto);
        });

        workflow.addStep(updatePaymentStep);
    }
    public PaymentDto getUpdatedPaymentDto() {
        return updatedPaymentDto;
    }

    public void setUpdatedPaymentDto(PaymentDto updatedPaymentDto) {
        this.updatedPaymentDto = updatedPaymentDto;
    }
}
