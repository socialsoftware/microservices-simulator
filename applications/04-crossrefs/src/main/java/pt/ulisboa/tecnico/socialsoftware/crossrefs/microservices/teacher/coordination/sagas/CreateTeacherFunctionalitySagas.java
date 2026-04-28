package pt.ulisboa.tecnico.socialsoftware.crossrefs.microservices.teacher.coordination.sagas;

import pt.ulisboa.tecnico.socialsoftware.ms.coordination.workflow.WorkflowFunctionality;
import pt.ulisboa.tecnico.socialsoftware.ms.coordination.workflow.command.CommandGateway;
import pt.ulisboa.tecnico.socialsoftware.crossrefs.ServiceMapping;
import pt.ulisboa.tecnico.socialsoftware.crossrefs.command.teacher.*;
import pt.ulisboa.tecnico.socialsoftware.crossrefs.shared.dtos.TeacherDto;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.aggregate.SagaAggregate.SagaState;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.unitOfWork.SagaUnitOfWork;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.unitOfWork.SagaUnitOfWorkService;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.workflow.SagaStep;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.workflow.SagaWorkflow;
import pt.ulisboa.tecnico.socialsoftware.crossrefs.microservices.teacher.aggregate.sagas.states.TeacherSagaState;
import pt.ulisboa.tecnico.socialsoftware.crossrefs.microservices.teacher.coordination.webapi.requestDtos.CreateTeacherRequestDto;

public class CreateTeacherFunctionalitySagas extends WorkflowFunctionality {
    private TeacherDto createdTeacherDto;
    private final SagaUnitOfWorkService unitOfWorkService;
    private final CommandGateway commandGateway;


    public CreateTeacherFunctionalitySagas(SagaUnitOfWorkService unitOfWorkService, CreateTeacherRequestDto createRequest, SagaUnitOfWork unitOfWork, CommandGateway commandGateway) {
        this.unitOfWorkService = unitOfWorkService;
        this.commandGateway = commandGateway;
        this.buildWorkflow(createRequest, unitOfWork);
    }

    public void buildWorkflow(CreateTeacherRequestDto createRequest, SagaUnitOfWork unitOfWork) {
        this.workflow = new SagaWorkflow(this, unitOfWorkService, unitOfWork);

        SagaStep createTeacherStep = new SagaStep("createTeacherStep", () -> {
            CreateTeacherCommand cmd = new CreateTeacherCommand(unitOfWork, ServiceMapping.TEACHER.getServiceName(), createRequest);
            TeacherDto createdTeacherDto = (TeacherDto) commandGateway.send(cmd);
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
