package pt.ulisboa.tecnico.socialsoftware.tutorial.sagas.coordination.loan;

import pt.ulisboa.tecnico.socialsoftware.ms.coordination.workflow.WorkflowFunctionality;
import pt.ulisboa.tecnico.socialsoftware.tutorial.microservices.loan.service.LoanService;
import pt.ulisboa.tecnico.socialsoftware.tutorial.shared.dtos.LoanDto;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.unitOfWork.SagaUnitOfWork;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.unitOfWork.SagaUnitOfWorkService;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.workflow.SagaSyncStep;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.workflow.SagaWorkflow;

public class UpdateLoanFunctionalitySagas extends WorkflowFunctionality {
    private LoanDto updatedLoanDto;
    private final LoanService loanService;
    private final SagaUnitOfWorkService unitOfWorkService;


    public UpdateLoanFunctionalitySagas(SagaUnitOfWork unitOfWork, SagaUnitOfWorkService unitOfWorkService, LoanService loanService, LoanDto loanDto) {
        this.loanService = loanService;
        this.unitOfWorkService = unitOfWorkService;
        this.buildWorkflow(loanDto, unitOfWork);
    }

    public void buildWorkflow(LoanDto loanDto, SagaUnitOfWork unitOfWork) {
        this.workflow = new SagaWorkflow(this, unitOfWorkService, unitOfWork);

        SagaSyncStep updateLoanStep = new SagaSyncStep("updateLoanStep", () -> {
            LoanDto updatedLoanDto = loanService.updateLoan(loanDto, unitOfWork);
            setUpdatedLoanDto(updatedLoanDto);
        });

        workflow.addStep(updateLoanStep);
    }
    public LoanDto getUpdatedLoanDto() {
        return updatedLoanDto;
    }

    public void setUpdatedLoanDto(LoanDto updatedLoanDto) {
        this.updatedLoanDto = updatedLoanDto;
    }
}
