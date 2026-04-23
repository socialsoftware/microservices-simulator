package pt.ulisboa.tecnico.socialsoftware.tutorial.microservices.loan.coordination.sagas;

import pt.ulisboa.tecnico.socialsoftware.ms.coordination.workflow.WorkflowFunctionality;
import pt.ulisboa.tecnico.socialsoftware.ms.coordination.workflow.command.CommandGateway;
import pt.ulisboa.tecnico.socialsoftware.tutorial.ServiceMapping;
import pt.ulisboa.tecnico.socialsoftware.tutorial.command.loan.*;
import pt.ulisboa.tecnico.socialsoftware.tutorial.shared.dtos.LoanDto;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.unitOfWork.SagaUnitOfWork;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.unitOfWork.SagaUnitOfWorkService;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.workflow.SagaStep;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.workflow.SagaWorkflow;

public class UpdateLoanFunctionalitySagas extends WorkflowFunctionality {
    private LoanDto updatedLoanDto;
    private final SagaUnitOfWorkService unitOfWorkService;
    private final CommandGateway commandGateway;


    public UpdateLoanFunctionalitySagas(SagaUnitOfWorkService unitOfWorkService, LoanDto loanDto, SagaUnitOfWork unitOfWork, CommandGateway commandGateway) {
        this.unitOfWorkService = unitOfWorkService;
        this.commandGateway = commandGateway;
        this.buildWorkflow(loanDto, unitOfWork);
    }

    public void buildWorkflow(LoanDto loanDto, SagaUnitOfWork unitOfWork) {
        this.workflow = new SagaWorkflow(this, unitOfWorkService, unitOfWork);

        SagaStep updateLoanStep = new SagaStep("updateLoanStep", () -> {
            UpdateLoanCommand cmd = new UpdateLoanCommand(unitOfWork, ServiceMapping.LOAN.getServiceName(), loanDto);
            LoanDto updatedLoanDto = (LoanDto) commandGateway.send(cmd);
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
