package pt.ulisboa.tecnico.socialsoftware.tutorial.sagas.coordination.loan;

import pt.ulisboa.tecnico.socialsoftware.ms.coordination.workflow.WorkflowFunctionality;
import pt.ulisboa.tecnico.socialsoftware.tutorial.microservices.loan.service.LoanService;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.unitOfWork.SagaUnitOfWork;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.unitOfWork.SagaUnitOfWorkService;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.workflow.SagaSyncStep;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.workflow.SagaWorkflow;

public class DeleteLoanFunctionalitySagas extends WorkflowFunctionality {
    private final LoanService loanService;
    private final SagaUnitOfWorkService unitOfWorkService;


    public DeleteLoanFunctionalitySagas(SagaUnitOfWork unitOfWork, SagaUnitOfWorkService unitOfWorkService, LoanService loanService, Integer loanAggregateId) {
        this.loanService = loanService;
        this.unitOfWorkService = unitOfWorkService;
        this.buildWorkflow(loanAggregateId, unitOfWork);
    }

    public void buildWorkflow(Integer loanAggregateId, SagaUnitOfWork unitOfWork) {
        this.workflow = new SagaWorkflow(this, unitOfWorkService, unitOfWork);

        SagaSyncStep deleteLoanStep = new SagaSyncStep("deleteLoanStep", () -> {
            loanService.deleteLoan(loanAggregateId, unitOfWork);
        });

        workflow.addStep(deleteLoanStep);
    }
}
