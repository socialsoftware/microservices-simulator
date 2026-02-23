package pt.ulisboa.tecnico.socialsoftware.tutorial.sagas.coordination.loan;

import pt.ulisboa.tecnico.socialsoftware.ms.coordination.workflow.WorkflowFunctionality;
import pt.ulisboa.tecnico.socialsoftware.tutorial.microservices.loan.service.LoanService;
import pt.ulisboa.tecnico.socialsoftware.tutorial.shared.dtos.LoanDto;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.unitOfWork.SagaUnitOfWork;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.unitOfWork.SagaUnitOfWorkService;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.workflow.SagaSyncStep;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.workflow.SagaWorkflow;
import pt.ulisboa.tecnico.socialsoftware.tutorial.coordination.webapi.requestDtos.CreateLoanRequestDto;

public class CreateLoanFunctionalitySagas extends WorkflowFunctionality {
    private LoanDto createdLoanDto;
    private final LoanService loanService;
    private final SagaUnitOfWorkService unitOfWorkService;


    public CreateLoanFunctionalitySagas(SagaUnitOfWork unitOfWork, SagaUnitOfWorkService unitOfWorkService, LoanService loanService, CreateLoanRequestDto createRequest) {
        this.loanService = loanService;
        this.unitOfWorkService = unitOfWorkService;
        this.buildWorkflow(createRequest, unitOfWork);
    }

    public void buildWorkflow(CreateLoanRequestDto createRequest, SagaUnitOfWork unitOfWork) {
        this.workflow = new SagaWorkflow(this, unitOfWorkService, unitOfWork);

        SagaSyncStep createLoanStep = new SagaSyncStep("createLoanStep", () -> {
            LoanDto createdLoanDto = loanService.createLoan(createRequest, unitOfWork);
            setCreatedLoanDto(createdLoanDto);
        });

        workflow.addStep(createLoanStep);
    }
    public LoanDto getCreatedLoanDto() {
        return createdLoanDto;
    }

    public void setCreatedLoanDto(LoanDto createdLoanDto) {
        this.createdLoanDto = createdLoanDto;
    }
}
