package pt.ulisboa.tecnico.socialsoftware.tutorial.command.loan;

import pt.ulisboa.tecnico.socialsoftware.ms.coordination.unitOfWork.UnitOfWork;
import pt.ulisboa.tecnico.socialsoftware.ms.coordination.workflow.Command;
import pt.ulisboa.tecnico.socialsoftware.tutorial.microservices.loan.coordination.webapi.requestDtos.CreateLoanRequestDto;

public class CreateLoanCommand extends Command {
    private final CreateLoanRequestDto createRequest;

    public CreateLoanCommand(UnitOfWork unitOfWork, String serviceName, CreateLoanRequestDto createRequest) {
        super(unitOfWork, serviceName, null);
        this.createRequest = createRequest;
    }

    public CreateLoanRequestDto getCreateRequest() { return createRequest; }
}
