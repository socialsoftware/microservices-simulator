package pt.ulisboa.tecnico.socialsoftware.tutorial.sagas.coordination.loan;

import pt.ulisboa.tecnico.socialsoftware.ms.coordination.workflow.WorkflowFunctionality;
import pt.ulisboa.tecnico.socialsoftware.tutorial.microservices.loan.service.LoanService;
import pt.ulisboa.tecnico.socialsoftware.tutorial.shared.dtos.LoanDto;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.unitOfWork.SagaUnitOfWork;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.unitOfWork.SagaUnitOfWorkService;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.workflow.SagaSyncStep;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.workflow.SagaWorkflow;
import java.util.List;

public class GetAllLoansFunctionalitySagas extends WorkflowFunctionality {
    private List<LoanDto> loans;
    private final LoanService loanService;
    private final SagaUnitOfWorkService unitOfWorkService;


    public GetAllLoansFunctionalitySagas(SagaUnitOfWork unitOfWork, SagaUnitOfWorkService unitOfWorkService, LoanService loanService) {
        this.loanService = loanService;
        this.unitOfWorkService = unitOfWorkService;
        this.buildWorkflow(unitOfWork);
    }

    public void buildWorkflow(SagaUnitOfWork unitOfWork) {
        this.workflow = new SagaWorkflow(this, unitOfWorkService, unitOfWork);

        SagaSyncStep getAllLoansStep = new SagaSyncStep("getAllLoansStep", () -> {
            List<LoanDto> loans = loanService.getAllLoans(unitOfWork);
            setLoans(loans);
        });

        workflow.addStep(getAllLoansStep);
    }
    public List<LoanDto> getLoans() {
        return loans;
    }

    public void setLoans(List<LoanDto> loans) {
        this.loans = loans;
    }
}
