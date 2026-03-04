package pt.ulisboa.tecnico.socialsoftware.crossrefs.microservices.enrollment.coordination.sagas;

import pt.ulisboa.tecnico.socialsoftware.ms.coordination.workflow.WorkflowFunctionality;
import pt.ulisboa.tecnico.socialsoftware.ms.coordination.workflow.command.CommandGateway;
import pt.ulisboa.tecnico.socialsoftware.crossrefs.ServiceMapping;
import pt.ulisboa.tecnico.socialsoftware.crossrefs.command.enrollment.*;
import pt.ulisboa.tecnico.socialsoftware.crossrefs.shared.dtos.EnrollmentDto;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.unitOfWork.SagaUnitOfWork;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.unitOfWork.SagaUnitOfWorkService;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.workflow.SagaStep;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.workflow.SagaWorkflow;

public class UpdateEnrollmentFunctionalitySagas extends WorkflowFunctionality {
    private EnrollmentDto updatedEnrollmentDto;
    private final SagaUnitOfWorkService unitOfWorkService;
    private final CommandGateway commandGateway;


    public UpdateEnrollmentFunctionalitySagas(SagaUnitOfWorkService unitOfWorkService, EnrollmentDto enrollmentDto, SagaUnitOfWork unitOfWork, CommandGateway commandGateway) {
        this.unitOfWorkService = unitOfWorkService;
        this.commandGateway = commandGateway;
        this.buildWorkflow(enrollmentDto, unitOfWork);
    }

    public void buildWorkflow(EnrollmentDto enrollmentDto, SagaUnitOfWork unitOfWork) {
        this.workflow = new SagaWorkflow(this, unitOfWorkService, unitOfWork);

        SagaStep updateEnrollmentStep = new SagaStep("updateEnrollmentStep", () -> {
            UpdateEnrollmentCommand cmd = new UpdateEnrollmentCommand(unitOfWork, ServiceMapping.ENROLLMENT.getServiceName(), enrollmentDto);
            EnrollmentDto updatedEnrollmentDto = (EnrollmentDto) commandGateway.send(cmd);
            setUpdatedEnrollmentDto(updatedEnrollmentDto);
        });

        workflow.addStep(updateEnrollmentStep);
    }
    public EnrollmentDto getUpdatedEnrollmentDto() {
        return updatedEnrollmentDto;
    }

    public void setUpdatedEnrollmentDto(EnrollmentDto updatedEnrollmentDto) {
        this.updatedEnrollmentDto = updatedEnrollmentDto;
    }
}
