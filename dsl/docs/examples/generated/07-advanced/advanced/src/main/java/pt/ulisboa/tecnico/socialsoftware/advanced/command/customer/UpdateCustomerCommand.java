package pt.ulisboa.tecnico.socialsoftware.advanced.command.customer;

import pt.ulisboa.tecnico.socialsoftware.ms.coordination.unitOfWork.UnitOfWork;
import pt.ulisboa.tecnico.socialsoftware.ms.coordination.workflow.Command;
import pt.ulisboa.tecnico.socialsoftware.advanced.shared.dtos.CustomerDto;

public class UpdateCustomerCommand extends Command {
    private final CustomerDto customerDto;

    public UpdateCustomerCommand(UnitOfWork unitOfWork, String serviceName, CustomerDto customerDto) {
        super(unitOfWork, serviceName, null);
        this.customerDto = customerDto;
    }

    public CustomerDto getCustomerDto() { return customerDto; }
}
