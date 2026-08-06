package pt.ulisboa.tecnico.socialsoftware.trainticket.command.payment;

import pt.ulisboa.tecnico.socialsoftware.ms.transaction.unitOfWork.UnitOfWork;
import pt.ulisboa.tecnico.socialsoftware.ms.messaging.Command;
import pt.ulisboa.tecnico.socialsoftware.trainticket.shared.dtos.PaymentDto;

public class UpdatePaymentCommand extends Command {
    private final PaymentDto paymentDto;

    public UpdatePaymentCommand(UnitOfWork unitOfWork, String serviceName, PaymentDto paymentDto) {
        super(unitOfWork, serviceName, null);
        this.paymentDto = paymentDto;
    }

    public PaymentDto getPaymentDto() { return paymentDto; }
}
