package pt.ulisboa.tecnico.socialsoftware.ecommerce.command.payment;

import pt.ulisboa.tecnico.socialsoftware.ms.coordination.unitOfWork.UnitOfWork;
import pt.ulisboa.tecnico.socialsoftware.ms.coordination.workflow.command.Command;
import pt.ulisboa.tecnico.socialsoftware.ecommerce.shared.dtos.PaymentDto;

public class UpdatePaymentCommand extends Command {
    private final PaymentDto paymentDto;

    public UpdatePaymentCommand(UnitOfWork unitOfWork, String serviceName, PaymentDto paymentDto) {
        super(unitOfWork, serviceName, null);
        this.paymentDto = paymentDto;
    }

    public PaymentDto getPaymentDto() { return paymentDto; }
}
