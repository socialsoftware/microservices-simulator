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

public class GetEnrollmentTeacherFunctionalitySagas extends WorkflowFunctionality {
    private EnrollmentTeacherDto teacherDto;
    private final SagaUnitOfWorkService unitOfWorkService;
    private final CommandGateway commandGateway;

    public GetEnrollmentTeacherFunctionalitySagas(SagaUnitOfWorkService unitOfWorkService, Integer enrollmentId, Integer teacherAggregateId, SagaUnitOfWork unitOfWork, CommandGateway commandGateway) {
        this.unitOfWorkService = unitOfWorkService;
        this.commandGateway = commandGateway;
        this.buildWorkflow(enrollmentId, teacherAggregateId, unitOfWork);
    }

    public void buildWorkflow(Integer enrollmentId, Integer teacherAggregateId, SagaUnitOfWork unitOfWork) {
        this.workflow = new SagaWorkflow(this, unitOfWorkService, unitOfWork);

        SagaStep getTeacherStep = new SagaStep("getTeacherStep", () -> {
            GetEnrollmentTeacherCommand cmd = new GetEnrollmentTeacherCommand(unitOfWork, ServiceMapping.ENROLLMENT.getServiceName(), enrollmentId, teacherAggregateId);
            EnrollmentTeacherDto teacherDto = (EnrollmentTeacherDto) commandGateway.send(cmd);
            setTeacherDto(teacherDto);
        });

        workflow.addStep(getTeacherStep);
    }
    public EnrollmentTeacherDto getTeacherDto() {
        return teacherDto;
    }

    public void setTeacherDto(EnrollmentTeacherDto teacherDto) {
        this.teacherDto = teacherDto;
    }
}
