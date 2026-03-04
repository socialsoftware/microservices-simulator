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
import java.util.List;

public class GetAllLoansFunctionalitySagas extends WorkflowFunctionality {
    private List<LoanDto> loans;
    private final SagaUnitOfWorkService unitOfWorkService;
    private final CommandGateway commandGateway;


    public GetAllLoansFunctionalitySagas(SagaUnitOfWorkService unitOfWorkService, SagaUnitOfWork unitOfWork, CommandGateway commandGateway) {
        this.unitOfWorkService = unitOfWorkService;
        this.commandGateway = commandGateway;
        this.buildWorkflow(unitOfWork);
    }

    public void buildWorkflow(SagaUnitOfWork unitOfWork) {
        this.workflow = new SagaWorkflow(this, unitOfWorkService, unitOfWork);

        SagaStep getAllLoansStep = new SagaStep("getAllLoansStep", () -> {
            GetAllLoansCommand cmd = new GetAllLoansCommand(unitOfWork, ServiceMapping.LOAN.getServiceName());
            List<LoanDto> loans = (List<LoanDto>) commandGateway.send(cmd);
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
