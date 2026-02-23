package pt.ulisboa.tecnico.socialsoftware.crossrefs.sagas.coordination.enrollment;

import pt.ulisboa.tecnico.socialsoftware.ms.coordination.workflow.WorkflowFunctionality;
import pt.ulisboa.tecnico.socialsoftware.crossrefs.microservices.enrollment.service.EnrollmentService;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.unitOfWork.SagaUnitOfWork;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.unitOfWork.SagaUnitOfWorkService;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.workflow.SagaSyncStep;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.workflow.SagaWorkflow;

public class RemoveEnrollmentTeacherFunctionalitySagas extends WorkflowFunctionality {
    private final EnrollmentService enrollmentService;
    private final SagaUnitOfWorkService unitOfWorkService;

    public RemoveEnrollmentTeacherFunctionalitySagas(SagaUnitOfWork unitOfWork, SagaUnitOfWorkService unitOfWorkService, EnrollmentService enrollmentService, Integer enrollmentId, Integer teacherAggregateId) {
        this.enrollmentService = enrollmentService;
        this.unitOfWorkService = unitOfWorkService;
        this.buildWorkflow(enrollmentId, teacherAggregateId, unitOfWork);
    }

    public void buildWorkflow(Integer enrollmentId, Integer teacherAggregateId, SagaUnitOfWork unitOfWork) {
        this.workflow = new SagaWorkflow(this, unitOfWorkService, unitOfWork);

        SagaSyncStep removeTeacherStep = new SagaSyncStep("removeTeacherStep", () -> {
            enrollmentService.removeEnrollmentTeacher(enrollmentId, teacherAggregateId, unitOfWork);
        });

        workflow.addStep(removeTeacherStep);
    }
}
