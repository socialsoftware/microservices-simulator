package pt.ulisboa.tecnico.socialsoftware.crossrefs.sagas.coordination.enrollment;

import pt.ulisboa.tecnico.socialsoftware.ms.coordination.workflow.WorkflowFunctionality;
import pt.ulisboa.tecnico.socialsoftware.crossrefs.microservices.enrollment.service.EnrollmentService;
import pt.ulisboa.tecnico.socialsoftware.crossrefs.shared.dtos.EnrollmentTeacherDto;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.unitOfWork.SagaUnitOfWork;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.unitOfWork.SagaUnitOfWorkService;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.workflow.SagaSyncStep;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.workflow.SagaWorkflow;

public class UpdateEnrollmentTeacherFunctionalitySagas extends WorkflowFunctionality {
    private EnrollmentTeacherDto updatedTeacherDto;
    private final EnrollmentService enrollmentService;
    private final SagaUnitOfWorkService unitOfWorkService;

    public UpdateEnrollmentTeacherFunctionalitySagas(SagaUnitOfWork unitOfWork, SagaUnitOfWorkService unitOfWorkService, EnrollmentService enrollmentService, Integer enrollmentId, Integer teacherAggregateId, EnrollmentTeacherDto teacherDto) {
        this.enrollmentService = enrollmentService;
        this.unitOfWorkService = unitOfWorkService;
        this.buildWorkflow(enrollmentId, teacherAggregateId, teacherDto, unitOfWork);
    }

    public void buildWorkflow(Integer enrollmentId, Integer teacherAggregateId, EnrollmentTeacherDto teacherDto, SagaUnitOfWork unitOfWork) {
        this.workflow = new SagaWorkflow(this, unitOfWorkService, unitOfWork);

        SagaSyncStep updateTeacherStep = new SagaSyncStep("updateTeacherStep", () -> {
            EnrollmentTeacherDto updatedTeacherDto = enrollmentService.updateEnrollmentTeacher(enrollmentId, teacherAggregateId, teacherDto, unitOfWork);
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
