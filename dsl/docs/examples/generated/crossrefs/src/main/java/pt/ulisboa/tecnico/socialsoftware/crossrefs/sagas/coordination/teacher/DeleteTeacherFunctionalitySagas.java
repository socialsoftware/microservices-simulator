package pt.ulisboa.tecnico.socialsoftware.crossrefs.sagas.coordination.teacher;

import pt.ulisboa.tecnico.socialsoftware.ms.coordination.workflow.WorkflowFunctionality;
import pt.ulisboa.tecnico.socialsoftware.crossrefs.microservices.teacher.service.TeacherService;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.unitOfWork.SagaUnitOfWork;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.unitOfWork.SagaUnitOfWorkService;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.workflow.SagaSyncStep;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.workflow.SagaWorkflow;

public class DeleteTeacherFunctionalitySagas extends WorkflowFunctionality {
    private final TeacherService teacherService;
    private final SagaUnitOfWorkService unitOfWorkService;


    public DeleteTeacherFunctionalitySagas(SagaUnitOfWork unitOfWork, SagaUnitOfWorkService unitOfWorkService, TeacherService teacherService, Integer teacherAggregateId) {
        this.teacherService = teacherService;
        this.unitOfWorkService = unitOfWorkService;
        this.buildWorkflow(teacherAggregateId, unitOfWork);
    }

    public void buildWorkflow(Integer teacherAggregateId, SagaUnitOfWork unitOfWork) {
        this.workflow = new SagaWorkflow(this, unitOfWorkService, unitOfWork);

        SagaSyncStep deleteTeacherStep = new SagaSyncStep("deleteTeacherStep", () -> {
            teacherService.deleteTeacher(teacherAggregateId, unitOfWork);
        });

        workflow.addStep(deleteTeacherStep);
    }
}
