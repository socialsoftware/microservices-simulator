package pt.ulisboa.tecnico.socialsoftware.tutorial.microservices.loan.coordination.sagas;

import pt.ulisboa.tecnico.socialsoftware.ms.coordination.workflow.WorkflowFunctionality;
import pt.ulisboa.tecnico.socialsoftware.ms.coordination.workflow.command.CommandGateway;
import pt.ulisboa.tecnico.socialsoftware.tutorial.ServiceMapping;
import pt.ulisboa.tecnico.socialsoftware.tutorial.command.loan.*;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.aggregate.SagaAggregate.SagaState;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.unitOfWork.SagaUnitOfWork;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.unitOfWork.SagaUnitOfWorkService;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.workflow.SagaStep;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.workflow.SagaWorkflow;
import pt.ulisboa.tecnico.socialsoftware.tutorial.microservices.loan.aggregate.sagas.states.LoanSagaState;

public class DeleteLoanFunctionalitySagas extends WorkflowFunctionality {
    private final SagaUnitOfWorkService unitOfWorkService;
    private final CommandGateway commandGateway;


    public DeleteLoanFunctionalitySagas(SagaUnitOfWorkService unitOfWorkService, Integer loanAggregateId, SagaUnitOfWork unitOfWork, CommandGateway commandGateway) {
        this.unitOfWorkService = unitOfWorkService;
        this.commandGateway = commandGateway;
        this.buildWorkflow(loanAggregateId, unitOfWork);
    }

    public void buildWorkflow(Integer loanAggregateId, SagaUnitOfWork unitOfWork) {
        this.workflow = new SagaWorkflow(this, unitOfWorkService, unitOfWork);

        SagaStep deleteLoanStep = new SagaStep("deleteLoanStep", () -> {
            unitOfWorkService.verifySagaState(loanAggregateId, new java.util.ArrayList<SagaState>(java.util.Arrays.asList(LoanSagaState.READ_LOAN, LoanSagaState.UPDATE_LOAN, LoanSagaState.DELETE_LOAN)));
            unitOfWorkService.registerSagaState(loanAggregateId, LoanSagaState.DELETE_LOAN, unitOfWork);
            DeleteLoanCommand cmd = new DeleteLoanCommand(unitOfWork, ServiceMapping.LOAN.getServiceName(), loanAggregateId);
            commandGateway.send(cmd);
        });

        workflow.addStep(deleteLoanStep);
    }
}
