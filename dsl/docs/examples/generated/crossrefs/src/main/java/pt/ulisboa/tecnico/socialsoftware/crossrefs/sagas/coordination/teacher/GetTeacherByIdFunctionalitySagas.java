package pt.ulisboa.tecnico.socialsoftware.crossrefs.sagas.coordination.teacher;

import pt.ulisboa.tecnico.socialsoftware.ms.coordination.workflow.WorkflowFunctionality;
import pt.ulisboa.tecnico.socialsoftware.crossrefs.microservices.teacher.service.TeacherService;
import pt.ulisboa.tecnico.socialsoftware.crossrefs.shared.dtos.TeacherDto;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.unitOfWork.SagaUnitOfWork;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.unitOfWork.SagaUnitOfWorkService;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.workflow.SagaSyncStep;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.workflow.SagaWorkflow;

public class GetTeacherByIdFunctionalitySagas extends WorkflowFunctionality {
    private TeacherDto teacherDto;
    private final TeacherService teacherService;
    private final SagaUnitOfWorkService unitOfWorkService;


    public GetTeacherByIdFunctionalitySagas(SagaUnitOfWork unitOfWork, SagaUnitOfWorkService unitOfWorkService, TeacherService teacherService, Integer teacherAggregateId) {
        this.teacherService = teacherService;
        this.unitOfWorkService = unitOfWorkService;
        this.buildWorkflow(teacherAggregateId, unitOfWork);
    }

    public void buildWorkflow(Integer teacherAggregateId, SagaUnitOfWork unitOfWork) {
        this.workflow = new SagaWorkflow(this, unitOfWorkService, unitOfWork);

        SagaSyncStep getTeacherStep = new SagaSyncStep("getTeacherStep", () -> {
            TeacherDto teacherDto = teacherService.getTeacherById(teacherAggregateId, unitOfWork);
            setTeacherDto(teacherDto);
        });

        workflow.addStep(getTeacherStep);
    }
    public TeacherDto getTeacherDto() {
        return teacherDto;
    }

    public void setTeacherDto(TeacherDto teacherDto) {
        this.teacherDto = teacherDto;
    }
}
