package pt.ulisboa.tecnico.socialsoftware.crossrefs.sagas.coordination.enrollment;

import pt.ulisboa.tecnico.socialsoftware.ms.coordination.workflow.WorkflowFunctionality;
import pt.ulisboa.tecnico.socialsoftware.crossrefs.microservices.enrollment.service.EnrollmentService;
import pt.ulisboa.tecnico.socialsoftware.crossrefs.shared.dtos.EnrollmentTeacherDto;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.unitOfWork.SagaUnitOfWork;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.unitOfWork.SagaUnitOfWorkService;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.workflow.SagaSyncStep;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.workflow.SagaWorkflow;
import java.util.List;

public class AddEnrollmentTeachersFunctionalitySagas extends WorkflowFunctionality {
    private List<EnrollmentTeacherDto> addedTeacherDtos;
    private final EnrollmentService enrollmentService;
    private final SagaUnitOfWorkService unitOfWorkService;

    public AddEnrollmentTeachersFunctionalitySagas(SagaUnitOfWork unitOfWork, SagaUnitOfWorkService unitOfWorkService, EnrollmentService enrollmentService, Integer enrollmentId, List<EnrollmentTeacherDto> teacherDtos) {
        this.enrollmentService = enrollmentService;
        this.unitOfWorkService = unitOfWorkService;
        this.buildWorkflow(enrollmentId, teacherDtos, unitOfWork);
    }

    public void buildWorkflow(Integer enrollmentId, List<EnrollmentTeacherDto> teacherDtos, SagaUnitOfWork unitOfWork) {
        this.workflow = new SagaWorkflow(this, unitOfWorkService, unitOfWork);

        SagaSyncStep addTeachersStep = new SagaSyncStep("addTeachersStep", () -> {
            List<EnrollmentTeacherDto> addedTeacherDtos = enrollmentService.addEnrollmentTeachers(enrollmentId, teacherDtos, unitOfWork);
            setAddedTeacherDtos(addedTeacherDtos);
        });

        workflow.addStep(addTeachersStep);
    }
    public List<EnrollmentTeacherDto> getAddedTeacherDtos() {
        return addedTeacherDtos;
    }

    public void setAddedTeacherDtos(List<EnrollmentTeacherDto> addedTeacherDtos) {
        this.addedTeacherDtos = addedTeacherDtos;
    }
}
