package pt.ulisboa.tecnico.socialsoftware.tutorial.command.loan;

import pt.ulisboa.tecnico.socialsoftware.ms.coordination.unitOfWork.UnitOfWork;
import pt.ulisboa.tecnico.socialsoftware.ms.coordination.workflow.command.Command;
import pt.ulisboa.tecnico.socialsoftware.tutorial.shared.dtos.LoanDto;

public class UpdateLoanCommand extends Command {
    private final LoanDto loanDto;

    public UpdateLoanCommand(UnitOfWork unitOfWork, String serviceName, LoanDto loanDto) {
        super(unitOfWork, serviceName, null);
        this.loanDto = loanDto;
    }

    public LoanDto getLoanDto() { return loanDto; }
}
