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
import pt.ulisboa.tecnico.socialsoftware.tutorial.microservices.loan.coordination.webapi.requestDtos.CreateLoanRequestDto;

public class CreateLoanFunctionalitySagas extends WorkflowFunctionality {
    private LoanDto createdLoanDto;
    private final SagaUnitOfWorkService unitOfWorkService;
    private final CommandGateway commandGateway;


    public CreateLoanFunctionalitySagas(SagaUnitOfWorkService unitOfWorkService, CreateLoanRequestDto createRequest, SagaUnitOfWork unitOfWork, CommandGateway commandGateway) {
        this.unitOfWorkService = unitOfWorkService;
        this.commandGateway = commandGateway;
        this.buildWorkflow(createRequest, unitOfWork);
    }

    public void buildWorkflow(CreateLoanRequestDto createRequest, SagaUnitOfWork unitOfWork) {
        this.workflow = new SagaWorkflow(this, unitOfWorkService, unitOfWork);

        SagaStep createLoanStep = new SagaStep("createLoanStep", () -> {
            CreateLoanCommand cmd = new CreateLoanCommand(unitOfWork, ServiceMapping.LOAN.getServiceName(), createRequest);
            LoanDto createdLoanDto = (LoanDto) commandGateway.send(cmd);
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
