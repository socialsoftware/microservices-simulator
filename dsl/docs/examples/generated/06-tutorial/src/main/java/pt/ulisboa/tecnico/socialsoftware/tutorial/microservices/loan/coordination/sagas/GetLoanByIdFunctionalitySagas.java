package pt.ulisboa.tecnico.socialsoftware.tutorial.microservices.loan.coordination.sagas;

import pt.ulisboa.tecnico.socialsoftware.ms.coordination.workflow.WorkflowFunctionality;
import pt.ulisboa.tecnico.socialsoftware.ms.coordination.workflow.CommandGateway;
import pt.ulisboa.tecnico.socialsoftware.tutorial.ServiceMapping;
import pt.ulisboa.tecnico.socialsoftware.tutorial.command.loan.*;
import pt.ulisboa.tecnico.socialsoftware.tutorial.shared.dtos.LoanDto;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.unitOfWork.SagaUnitOfWork;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.unitOfWork.SagaUnitOfWorkService;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.workflow.SagaStep;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.workflow.SagaWorkflow;

public class GetLoanByIdFunctionalitySagas extends WorkflowFunctionality {
    private LoanDto loanDto;
    private final SagaUnitOfWorkService unitOfWorkService;
    private final CommandGateway commandGateway;


    public GetLoanByIdFunctionalitySagas(SagaUnitOfWorkService unitOfWorkService, Integer loanAggregateId, SagaUnitOfWork unitOfWork, CommandGateway commandGateway) {
        this.unitOfWorkService = unitOfWorkService;
        this.commandGateway = commandGateway;
        this.buildWorkflow(loanAggregateId, unitOfWork);
    }

    public void buildWorkflow(Integer loanAggregateId, SagaUnitOfWork unitOfWork) {
        this.workflow = new SagaWorkflow(this, unitOfWorkService, unitOfWork);

        SagaStep getLoanStep = new SagaStep("getLoanStep", () -> {
            GetLoanByIdCommand cmd = new GetLoanByIdCommand(unitOfWork, ServiceMapping.LOAN.getServiceName(), loanAggregateId);
            LoanDto loanDto = (LoanDto) commandGateway.send(cmd);
            setLoanDto(loanDto);
        });

        workflow.addStep(getLoanStep);
    }
    public LoanDto getLoanDto() {
        return loanDto;
    }

    public void setLoanDto(LoanDto loanDto) {
        this.loanDto = loanDto;
    }
}
