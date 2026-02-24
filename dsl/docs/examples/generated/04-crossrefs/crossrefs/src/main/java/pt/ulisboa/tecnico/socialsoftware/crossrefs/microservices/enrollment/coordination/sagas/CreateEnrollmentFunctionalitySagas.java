package pt.ulisboa.tecnico.socialsoftware.crossrefs.microservices.enrollment.coordination.sagas;

import pt.ulisboa.tecnico.socialsoftware.ms.coordination.workflow.WorkflowFunctionality;
import pt.ulisboa.tecnico.socialsoftware.ms.coordination.workflow.CommandGateway;
import pt.ulisboa.tecnico.socialsoftware.crossrefs.ServiceMapping;
import pt.ulisboa.tecnico.socialsoftware.crossrefs.command.enrollment.*;
import pt.ulisboa.tecnico.socialsoftware.crossrefs.shared.dtos.EnrollmentDto;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.unitOfWork.SagaUnitOfWork;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.unitOfWork.SagaUnitOfWorkService;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.workflow.SagaStep;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.workflow.SagaWorkflow;
import pt.ulisboa.tecnico.socialsoftware.crossrefs.microservices.enrollment.coordination.webapi.requestDtos.CreateEnrollmentRequestDto;

public class CreateEnrollmentFunctionalitySagas extends WorkflowFunctionality {
    private EnrollmentDto createdEnrollmentDto;
    private final SagaUnitOfWorkService unitOfWorkService;
    private final CommandGateway commandGateway;


    public CreateEnrollmentFunctionalitySagas(SagaUnitOfWorkService unitOfWorkService, CreateEnrollmentRequestDto createRequest, SagaUnitOfWork unitOfWork, CommandGateway commandGateway) {
        this.unitOfWorkService = unitOfWorkService;
        this.commandGateway = commandGateway;
        this.buildWorkflow(createRequest, unitOfWork);
    }

    public void buildWorkflow(CreateEnrollmentRequestDto createRequest, SagaUnitOfWork unitOfWork) {
        this.workflow = new SagaWorkflow(this, unitOfWorkService, unitOfWork);

        SagaStep createEnrollmentStep = new SagaStep("createEnrollmentStep", () -> {
            CreateEnrollmentCommand cmd = new CreateEnrollmentCommand(unitOfWork, ServiceMapping.ENROLLMENT.getServiceName(), createRequest);
            EnrollmentDto createdEnrollmentDto = (EnrollmentDto) commandGateway.send(cmd);
            setCreatedEnrollmentDto(createdEnrollmentDto);
        });

        workflow.addStep(createEnrollmentStep);
    }
    public EnrollmentDto getCreatedEnrollmentDto() {
        return createdEnrollmentDto;
    }

    public void setCreatedEnrollmentDto(EnrollmentDto createdEnrollmentDto) {
        this.createdEnrollmentDto = createdEnrollmentDto;
    }
}
