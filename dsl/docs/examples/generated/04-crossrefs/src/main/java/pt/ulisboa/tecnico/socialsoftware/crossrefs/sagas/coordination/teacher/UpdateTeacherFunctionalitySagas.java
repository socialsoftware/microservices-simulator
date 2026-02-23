package pt.ulisboa.tecnico.socialsoftware.crossrefs.sagas.coordination.teacher;

import pt.ulisboa.tecnico.socialsoftware.ms.coordination.workflow.WorkflowFunctionality;
import pt.ulisboa.tecnico.socialsoftware.crossrefs.microservices.teacher.service.TeacherService;
import pt.ulisboa.tecnico.socialsoftware.crossrefs.shared.dtos.TeacherDto;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.unitOfWork.SagaUnitOfWork;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.unitOfWork.SagaUnitOfWorkService;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.workflow.SagaSyncStep;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.workflow.SagaWorkflow;

public class UpdateTeacherFunctionalitySagas extends WorkflowFunctionality {
    private TeacherDto updatedTeacherDto;
    private final TeacherService teacherService;
    private final SagaUnitOfWorkService unitOfWorkService;


    public UpdateTeacherFunctionalitySagas(SagaUnitOfWork unitOfWork, SagaUnitOfWorkService unitOfWorkService, TeacherService teacherService, TeacherDto teacherDto) {
        this.teacherService = teacherService;
        this.unitOfWorkService = unitOfWorkService;
        this.buildWorkflow(teacherDto, unitOfWork);
    }

    public void buildWorkflow(TeacherDto teacherDto, SagaUnitOfWork unitOfWork) {
        this.workflow = new SagaWorkflow(this, unitOfWorkService, unitOfWork);

        SagaSyncStep updateTeacherStep = new SagaSyncStep("updateTeacherStep", () -> {
            TeacherDto updatedTeacherDto = teacherService.updateTeacher(teacherDto, unitOfWork);
            setUpdatedTeacherDto(updatedTeacherDto);
        });

        workflow.addStep(updateTeacherStep);
    }
    public TeacherDto getUpdatedTeacherDto() {
        return updatedTeacherDto;
    }

    public void setUpdatedTeacherDto(TeacherDto updatedTeacherDto) {
        this.updatedTeacherDto = updatedTeacherDto;
    }
}
