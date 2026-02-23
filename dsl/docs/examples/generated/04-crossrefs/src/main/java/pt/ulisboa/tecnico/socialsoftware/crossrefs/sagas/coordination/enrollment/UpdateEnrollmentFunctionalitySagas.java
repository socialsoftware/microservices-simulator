package pt.ulisboa.tecnico.socialsoftware.crossrefs.sagas.coordination.enrollment;

import pt.ulisboa.tecnico.socialsoftware.ms.coordination.workflow.WorkflowFunctionality;
import pt.ulisboa.tecnico.socialsoftware.crossrefs.microservices.enrollment.service.EnrollmentService;
import pt.ulisboa.tecnico.socialsoftware.crossrefs.shared.dtos.EnrollmentDto;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.unitOfWork.SagaUnitOfWork;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.unitOfWork.SagaUnitOfWorkService;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.workflow.SagaSyncStep;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.workflow.SagaWorkflow;

public class UpdateEnrollmentFunctionalitySagas extends WorkflowFunctionality {
    private EnrollmentDto updatedEnrollmentDto;
    private final EnrollmentService enrollmentService;
    private final SagaUnitOfWorkService unitOfWorkService;


    public UpdateEnrollmentFunctionalitySagas(SagaUnitOfWork unitOfWork, SagaUnitOfWorkService unitOfWorkService, EnrollmentService enrollmentService, EnrollmentDto enrollmentDto) {
        this.enrollmentService = enrollmentService;
        this.unitOfWorkService = unitOfWorkService;
        this.buildWorkflow(enrollmentDto, unitOfWork);
    }

    public void buildWorkflow(EnrollmentDto enrollmentDto, SagaUnitOfWork unitOfWork) {
        this.workflow = new SagaWorkflow(this, unitOfWorkService, unitOfWork);

        SagaSyncStep updateEnrollmentStep = new SagaSyncStep("updateEnrollmentStep", () -> {
            EnrollmentDto updatedEnrollmentDto = enrollmentService.updateEnrollment(enrollmentDto, unitOfWork);
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
