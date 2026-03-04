package pt.ulisboa.tecnico.socialsoftware.crossrefs.microservices.enrollment.coordination.sagas;

import pt.ulisboa.tecnico.socialsoftware.ms.coordination.workflow.WorkflowFunctionality;
import pt.ulisboa.tecnico.socialsoftware.ms.coordination.workflow.CommandGateway;
import pt.ulisboa.tecnico.socialsoftware.crossrefs.ServiceMapping;
import pt.ulisboa.tecnico.socialsoftware.crossrefs.command.enrollment.*;
import pt.ulisboa.tecnico.socialsoftware.crossrefs.shared.dtos.EnrollmentTeacherDto;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.unitOfWork.SagaUnitOfWork;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.unitOfWork.SagaUnitOfWorkService;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.workflow.SagaStep;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.workflow.SagaWorkflow;

public class UpdateEnrollmentTeacherFunctionalitySagas extends WorkflowFunctionality {
    private EnrollmentTeacherDto updatedTeacherDto;
    private final SagaUnitOfWorkService unitOfWorkService;
    private final CommandGateway commandGateway;

    public UpdateEnrollmentTeacherFunctionalitySagas(SagaUnitOfWorkService unitOfWorkService, Integer enrollmentId, Integer teacherAggregateId, EnrollmentTeacherDto teacherDto, SagaUnitOfWork unitOfWork, CommandGateway commandGateway) {
        this.unitOfWorkService = unitOfWorkService;
        this.commandGateway = commandGateway;
        this.buildWorkflow(enrollmentId, teacherAggregateId, teacherDto, unitOfWork);
    }

    public void buildWorkflow(Integer enrollmentId, Integer teacherAggregateId, EnrollmentTeacherDto teacherDto, SagaUnitOfWork unitOfWork) {
        this.workflow = new SagaWorkflow(this, unitOfWorkService, unitOfWork);

        SagaStep updateTeacherStep = new SagaStep("updateTeacherStep", () -> {
            UpdateEnrollmentTeacherCommand cmd = new UpdateEnrollmentTeacherCommand(unitOfWork, ServiceMapping.ENROLLMENT.getServiceName(), enrollmentId, teacherAggregateId, teacherDto);
            EnrollmentTeacherDto updatedTeacherDto = (EnrollmentTeacherDto) commandGateway.send(cmd);
            setUpdatedTeacherDto(updatedTeacherDto);
        });

        workflow.addStep(updateTeacherStep);
    }
    public EnrollmentTeacherDto getUpdatedTeacherDto() {
        return updatedTeacherDto;
    }

    public void setUpdatedTeacherDto(EnrollmentTeacherDto updatedTeacherDto) {
        this.updatedTeacherDto = updatedTeacherDto;
    }
}
