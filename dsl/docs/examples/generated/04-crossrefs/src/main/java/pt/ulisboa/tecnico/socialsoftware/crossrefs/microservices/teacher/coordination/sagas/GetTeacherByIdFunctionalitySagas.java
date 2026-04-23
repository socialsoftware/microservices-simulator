package pt.ulisboa.tecnico.socialsoftware.crossrefs.microservices.teacher.coordination.sagas;

import pt.ulisboa.tecnico.socialsoftware.ms.coordination.workflow.WorkflowFunctionality;
import pt.ulisboa.tecnico.socialsoftware.ms.coordination.workflow.command.CommandGateway;
import pt.ulisboa.tecnico.socialsoftware.crossrefs.ServiceMapping;
import pt.ulisboa.tecnico.socialsoftware.crossrefs.command.teacher.*;
import pt.ulisboa.tecnico.socialsoftware.crossrefs.shared.dtos.TeacherDto;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.unitOfWork.SagaUnitOfWork;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.unitOfWork.SagaUnitOfWorkService;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.workflow.SagaStep;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.workflow.SagaWorkflow;

public class GetTeacherByIdFunctionalitySagas extends WorkflowFunctionality {
    private TeacherDto teacherDto;
    private final SagaUnitOfWorkService unitOfWorkService;
    private final CommandGateway commandGateway;


    public GetTeacherByIdFunctionalitySagas(SagaUnitOfWorkService unitOfWorkService, Integer teacherAggregateId, SagaUnitOfWork unitOfWork, CommandGateway commandGateway) {
        this.unitOfWorkService = unitOfWorkService;
        this.commandGateway = commandGateway;
        this.buildWorkflow(teacherAggregateId, unitOfWork);
    }

    public void buildWorkflow(Integer teacherAggregateId, SagaUnitOfWork unitOfWork) {
        this.workflow = new SagaWorkflow(this, unitOfWorkService, unitOfWork);

        SagaStep getTeacherStep = new SagaStep("getTeacherStep", () -> {
            GetTeacherByIdCommand cmd = new GetTeacherByIdCommand(unitOfWork, ServiceMapping.TEACHER.getServiceName(), teacherAggregateId);
            TeacherDto teacherDto = (TeacherDto) commandGateway.send(cmd);
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
