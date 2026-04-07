package pt.ulisboa.tecnico.socialsoftware.ecommerce.command.payment;

import pt.ulisboa.tecnico.socialsoftware.ms.coordination.unitOfWork.UnitOfWork;
import pt.ulisboa.tecnico.socialsoftware.ms.coordination.workflow.command.Command;
import pt.ulisboa.tecnico.socialsoftware.ecommerce.microservices.payment.coordination.webapi.requestDtos.CreatePaymentRequestDto;

public class CreatePaymentCommand extends Command {
    private final CreatePaymentRequestDto createRequest;

    public CreatePaymentCommand(UnitOfWork unitOfWork, String serviceName, CreatePaymentRequestDto createRequest) {
        super(unitOfWork, serviceName, null);
        this.createRequest = createRequest;
    }

    public CreatePaymentRequestDto getCreateRequest() { return createRequest; }
}
