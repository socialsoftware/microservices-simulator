package pt.ulisboa.tecnico.socialsoftware.crossrefs.sagas.coordination.enrollment;

import pt.ulisboa.tecnico.socialsoftware.ms.coordination.workflow.WorkflowFunctionality;
import pt.ulisboa.tecnico.socialsoftware.crossrefs.microservices.enrollment.service.EnrollmentService;
import pt.ulisboa.tecnico.socialsoftware.crossrefs.shared.dtos.EnrollmentDto;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.unitOfWork.SagaUnitOfWork;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.unitOfWork.SagaUnitOfWorkService;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.workflow.SagaSyncStep;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.workflow.SagaWorkflow;
import pt.ulisboa.tecnico.socialsoftware.crossrefs.coordination.webapi.requestDtos.CreateEnrollmentRequestDto;

public class CreateEnrollmentFunctionalitySagas extends WorkflowFunctionality {
    private EnrollmentDto createdEnrollmentDto;
    private final EnrollmentService enrollmentService;
    private final SagaUnitOfWorkService unitOfWorkService;


    public CreateEnrollmentFunctionalitySagas(SagaUnitOfWork unitOfWork, SagaUnitOfWorkService unitOfWorkService, EnrollmentService enrollmentService, CreateEnrollmentRequestDto createRequest) {
        this.enrollmentService = enrollmentService;
        this.unitOfWorkService = unitOfWorkService;
        this.buildWorkflow(createRequest, unitOfWork);
    }

    public void buildWorkflow(CreateEnrollmentRequestDto createRequest, SagaUnitOfWork unitOfWork) {
        this.workflow = new SagaWorkflow(this, unitOfWorkService, unitOfWork);

        SagaSyncStep createEnrollmentStep = new SagaSyncStep("createEnrollmentStep", () -> {
            EnrollmentDto createdEnrollmentDto = enrollmentService.createEnrollment(createRequest, unitOfWork);
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
