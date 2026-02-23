package pt.ulisboa.tecnico.socialsoftware.crossrefs.sagas.coordination.teacher;

import pt.ulisboa.tecnico.socialsoftware.ms.coordination.workflow.WorkflowFunctionality;
import pt.ulisboa.tecnico.socialsoftware.crossrefs.microservices.teacher.service.TeacherService;
import pt.ulisboa.tecnico.socialsoftware.crossrefs.shared.dtos.TeacherDto;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.unitOfWork.SagaUnitOfWork;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.unitOfWork.SagaUnitOfWorkService;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.workflow.SagaSyncStep;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.workflow.SagaWorkflow;
import pt.ulisboa.tecnico.socialsoftware.crossrefs.coordination.webapi.requestDtos.CreateTeacherRequestDto;

public class CreateTeacherFunctionalitySagas extends WorkflowFunctionality {
    private TeacherDto createdTeacherDto;
    private final TeacherService teacherService;
    private final SagaUnitOfWorkService unitOfWorkService;


    public CreateTeacherFunctionalitySagas(SagaUnitOfWork unitOfWork, SagaUnitOfWorkService unitOfWorkService, TeacherService teacherService, CreateTeacherRequestDto createRequest) {
        this.teacherService = teacherService;
        this.unitOfWorkService = unitOfWorkService;
        this.buildWorkflow(createRequest, unitOfWork);
    }

    public void buildWorkflow(CreateTeacherRequestDto createRequest, SagaUnitOfWork unitOfWork) {
        this.workflow = new SagaWorkflow(this, unitOfWorkService, unitOfWork);

        SagaSyncStep createTeacherStep = new SagaSyncStep("createTeacherStep", () -> {
            TeacherDto createdTeacherDto = teacherService.createTeacher(createRequest, unitOfWork);
            setCreatedTeacherDto(createdTeacherDto);
        });

        workflow.addStep(createTeacherStep);
    }
    public TeacherDto getCreatedTeacherDto() {
        return createdTeacherDto;
    }

    public void setCreatedTeacherDto(TeacherDto createdTeacherDto) {
        this.createdTeacherDto = createdTeacherDto;
    }
}
