package pt.ulisboa.tecnico.socialsoftware.crossrefs.sagas.coordination.enrollment;

import pt.ulisboa.tecnico.socialsoftware.ms.coordination.workflow.WorkflowFunctionality;
import pt.ulisboa.tecnico.socialsoftware.crossrefs.microservices.enrollment.service.EnrollmentService;
import pt.ulisboa.tecnico.socialsoftware.crossrefs.shared.dtos.EnrollmentDto;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.unitOfWork.SagaUnitOfWork;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.unitOfWork.SagaUnitOfWorkService;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.workflow.SagaSyncStep;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.workflow.SagaWorkflow;
import java.util.List;

public class GetAllEnrollmentsFunctionalitySagas extends WorkflowFunctionality {
    private List<EnrollmentDto> enrollments;
    private final EnrollmentService enrollmentService;
    private final SagaUnitOfWorkService unitOfWorkService;


    public GetAllEnrollmentsFunctionalitySagas(SagaUnitOfWork unitOfWork, SagaUnitOfWorkService unitOfWorkService, EnrollmentService enrollmentService) {
        this.enrollmentService = enrollmentService;
        this.unitOfWorkService = unitOfWorkService;
        this.buildWorkflow(unitOfWork);
    }

    public void buildWorkflow(SagaUnitOfWork unitOfWork) {
        this.workflow = new SagaWorkflow(this, unitOfWorkService, unitOfWork);

        SagaSyncStep getAllEnrollmentsStep = new SagaSyncStep("getAllEnrollmentsStep", () -> {
            List<EnrollmentDto> enrollments = enrollmentService.getAllEnrollments(unitOfWork);
            setEnrollments(enrollments);
        });

        workflow.addStep(getAllEnrollmentsStep);
    }
    public List<EnrollmentDto> getEnrollments() {
        return enrollments;
    }

    public void setEnrollments(List<EnrollmentDto> enrollments) {
        this.enrollments = enrollments;
    }
}
