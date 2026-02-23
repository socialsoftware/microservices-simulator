package pt.ulisboa.tecnico.socialsoftware.crossrefs.sagas.coordination.enrollment;

import pt.ulisboa.tecnico.socialsoftware.ms.coordination.workflow.WorkflowFunctionality;
import pt.ulisboa.tecnico.socialsoftware.crossrefs.microservices.enrollment.service.EnrollmentService;
import pt.ulisboa.tecnico.socialsoftware.crossrefs.shared.dtos.EnrollmentDto;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.unitOfWork.SagaUnitOfWork;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.unitOfWork.SagaUnitOfWorkService;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.workflow.SagaSyncStep;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.workflow.SagaWorkflow;

public class GetEnrollmentByIdFunctionalitySagas extends WorkflowFunctionality {
    private EnrollmentDto enrollmentDto;
    private final EnrollmentService enrollmentService;
    private final SagaUnitOfWorkService unitOfWorkService;


    public GetEnrollmentByIdFunctionalitySagas(SagaUnitOfWork unitOfWork, SagaUnitOfWorkService unitOfWorkService, EnrollmentService enrollmentService, Integer enrollmentAggregateId) {
        this.enrollmentService = enrollmentService;
        this.unitOfWorkService = unitOfWorkService;
        this.buildWorkflow(enrollmentAggregateId, unitOfWork);
    }

    public void buildWorkflow(Integer enrollmentAggregateId, SagaUnitOfWork unitOfWork) {
        this.workflow = new SagaWorkflow(this, unitOfWorkService, unitOfWork);

        SagaSyncStep getEnrollmentStep = new SagaSyncStep("getEnrollmentStep", () -> {
            EnrollmentDto enrollmentDto = enrollmentService.getEnrollmentById(enrollmentAggregateId, unitOfWork);
            setEnrollmentDto(enrollmentDto);
        });

        workflow.addStep(getEnrollmentStep);
    }
    public EnrollmentDto getEnrollmentDto() {
        return enrollmentDto;
    }

    public void setEnrollmentDto(EnrollmentDto enrollmentDto) {
        this.enrollmentDto = enrollmentDto;
    }
}
