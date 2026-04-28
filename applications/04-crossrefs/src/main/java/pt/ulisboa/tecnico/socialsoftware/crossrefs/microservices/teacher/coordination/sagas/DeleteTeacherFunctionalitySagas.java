package pt.ulisboa.tecnico.socialsoftware.crossrefs.microservices.teacher.coordination.sagas;

import pt.ulisboa.tecnico.socialsoftware.ms.coordination.workflow.WorkflowFunctionality;
import pt.ulisboa.tecnico.socialsoftware.ms.coordination.workflow.command.CommandGateway;
import pt.ulisboa.tecnico.socialsoftware.crossrefs.ServiceMapping;
import pt.ulisboa.tecnico.socialsoftware.crossrefs.command.teacher.*;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.aggregate.SagaAggregate.SagaState;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.unitOfWork.SagaUnitOfWork;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.unitOfWork.SagaUnitOfWorkService;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.workflow.SagaStep;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.workflow.SagaWorkflow;
import pt.ulisboa.tecnico.socialsoftware.crossrefs.microservices.teacher.aggregate.sagas.states.TeacherSagaState;

public class DeleteTeacherFunctionalitySagas extends WorkflowFunctionality {
    private final SagaUnitOfWorkService unitOfWorkService;
    private final CommandGateway commandGateway;


    public DeleteTeacherFunctionalitySagas(SagaUnitOfWorkService unitOfWorkService, Integer teacherAggregateId, SagaUnitOfWork unitOfWork, CommandGateway commandGateway) {
        this.unitOfWorkService = unitOfWorkService;
        this.commandGateway = commandGateway;
        this.buildWorkflow(teacherAggregateId, unitOfWork);
    }

    public void buildWorkflow(Integer teacherAggregateId, SagaUnitOfWork unitOfWork) {
        this.workflow = new SagaWorkflow(this, unitOfWorkService, unitOfWork);

        SagaStep deleteTeacherStep = new SagaStep("deleteTeacherStep", () -> {
            unitOfWorkService.verifySagaState(teacherAggregateId, new java.util.ArrayList<SagaState>(java.util.Arrays.asList(TeacherSagaState.READ_TEACHER, TeacherSagaState.UPDATE_TEACHER, TeacherSagaState.DELETE_TEACHER)));
            unitOfWorkService.registerSagaState(teacherAggregateId, TeacherSagaState.DELETE_TEACHER, unitOfWork);
            DeleteTeacherCommand cmd = new DeleteTeacherCommand(unitOfWork, ServiceMapping.TEACHER.getServiceName(), teacherAggregateId);
            commandGateway.send(cmd);
        });

        workflow.addStep(deleteTeacherStep);
    }
}
