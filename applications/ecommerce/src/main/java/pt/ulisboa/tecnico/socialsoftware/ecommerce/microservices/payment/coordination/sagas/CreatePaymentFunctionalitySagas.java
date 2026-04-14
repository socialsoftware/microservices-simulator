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
import pt.ulisboa.tecnico.socialsoftware.ecommerce.microservices.payment.coordination.webapi.requestDtos.CreatePaymentRequestDto;

public class CreatePaymentFunctionalitySagas extends WorkflowFunctionality {
    private PaymentDto createdPaymentDto;
    private final SagaUnitOfWorkService unitOfWorkService;
    private final CommandGateway commandGateway;


    public CreatePaymentFunctionalitySagas(SagaUnitOfWorkService unitOfWorkService, CreatePaymentRequestDto createRequest, SagaUnitOfWork unitOfWork, CommandGateway commandGateway) {
        this.unitOfWorkService = unitOfWorkService;
        this.commandGateway = commandGateway;
        this.buildWorkflow(createRequest, unitOfWork);
    }

    public void buildWorkflow(CreatePaymentRequestDto createRequest, SagaUnitOfWork unitOfWork) {
        this.workflow = new SagaWorkflow(this, unitOfWorkService, unitOfWork);

        SagaStep createPaymentStep = new SagaStep("createPaymentStep", () -> {
            CreatePaymentCommand cmd = new CreatePaymentCommand(unitOfWork, ServiceMapping.PAYMENT.getServiceName(), createRequest);
            PaymentDto createdPaymentDto = (PaymentDto) commandGateway.send(cmd);
            setCreatedPaymentDto(createdPaymentDto);
        });

        workflow.addStep(createPaymentStep);
    }
    public PaymentDto getCreatedPaymentDto() {
        return createdPaymentDto;
    }

    public void setCreatedPaymentDto(PaymentDto createdPaymentDto) {
        this.createdPaymentDto = createdPaymentDto;
    }
}
