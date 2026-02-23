package pt.ulisboa.tecnico.socialsoftware.crossrefs.sagas.coordination.teacher;

import pt.ulisboa.tecnico.socialsoftware.ms.coordination.workflow.WorkflowFunctionality;
import pt.ulisboa.tecnico.socialsoftware.crossrefs.microservices.teacher.service.TeacherService;
import pt.ulisboa.tecnico.socialsoftware.crossrefs.shared.dtos.TeacherDto;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.unitOfWork.SagaUnitOfWork;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.unitOfWork.SagaUnitOfWorkService;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.workflow.SagaSyncStep;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.workflow.SagaWorkflow;
import java.util.List;

public class GetAllTeachersFunctionalitySagas extends WorkflowFunctionality {
    private List<TeacherDto> teachers;
    private final TeacherService teacherService;
    private final SagaUnitOfWorkService unitOfWorkService;


    public GetAllTeachersFunctionalitySagas(SagaUnitOfWork unitOfWork, SagaUnitOfWorkService unitOfWorkService, TeacherService teacherService) {
        this.teacherService = teacherService;
        this.unitOfWorkService = unitOfWorkService;
        this.buildWorkflow(unitOfWork);
    }

    public void buildWorkflow(SagaUnitOfWork unitOfWork) {
        this.workflow = new SagaWorkflow(this, unitOfWorkService, unitOfWork);

        SagaSyncStep getAllTeachersStep = new SagaSyncStep("getAllTeachersStep", () -> {
            List<TeacherDto> teachers = teacherService.getAllTeachers(unitOfWork);
            setTeachers(teachers);
        });

        workflow.addStep(getAllTeachersStep);
    }
    public List<TeacherDto> getTeachers() {
        return teachers;
    }

    public void setTeachers(List<TeacherDto> teachers) {
        this.teachers = teachers;
    }
}
