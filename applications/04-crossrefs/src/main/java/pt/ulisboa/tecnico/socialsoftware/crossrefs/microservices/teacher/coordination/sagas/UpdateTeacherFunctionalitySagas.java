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

public class UpdateTeacherFunctionalitySagas extends WorkflowFunctionality {
    private TeacherDto updatedTeacherDto;
    private final SagaUnitOfWorkService unitOfWorkService;
    private final CommandGateway commandGateway;


    public UpdateTeacherFunctionalitySagas(SagaUnitOfWorkService unitOfWorkService, TeacherDto teacherDto, SagaUnitOfWork unitOfWork, CommandGateway commandGateway) {
        this.unitOfWorkService = unitOfWorkService;
        this.commandGateway = commandGateway;
        this.buildWorkflow(teacherDto, unitOfWork);
    }

    public void buildWorkflow(TeacherDto teacherDto, SagaUnitOfWork unitOfWork) {
        this.workflow = new SagaWorkflow(this, unitOfWorkService, unitOfWork);

        SagaStep updateTeacherStep = new SagaStep("updateTeacherStep", () -> {
            unitOfWorkService.verifySagaState(teacherDto.getAggregateId(), new java.util.ArrayList<SagaState>(java.util.Arrays.asList(TeacherSagaState.READ_TEACHER, TeacherSagaState.UPDATE_TEACHER, TeacherSagaState.DELETE_TEACHER)));
            unitOfWorkService.registerSagaState(teacherDto.getAggregateId(), TeacherSagaState.UPDATE_TEACHER, unitOfWork);
            UpdateTeacherCommand cmd = new UpdateTeacherCommand(unitOfWork, ServiceMapping.TEACHER.getServiceName(), teacherDto);
            TeacherDto updatedTeacherDto = (TeacherDto) commandGateway.send(cmd);
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
