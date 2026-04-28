package pt.ulisboa.tecnico.socialsoftware.answers.microservices.course.coordination.sagas;

import pt.ulisboa.tecnico.socialsoftware.ms.coordination.workflow.WorkflowFunctionality;
import pt.ulisboa.tecnico.socialsoftware.ms.coordination.workflow.command.CommandGateway;
import pt.ulisboa.tecnico.socialsoftware.answers.ServiceMapping;
import pt.ulisboa.tecnico.socialsoftware.answers.command.course.*;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.aggregate.SagaAggregate.SagaState;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.unitOfWork.SagaUnitOfWork;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.unitOfWork.SagaUnitOfWorkService;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.workflow.SagaStep;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.workflow.SagaWorkflow;
import pt.ulisboa.tecnico.socialsoftware.answers.microservices.course.aggregate.sagas.states.CourseSagaState;

public class DeleteCourseFunctionalitySagas extends WorkflowFunctionality {
    private final SagaUnitOfWorkService unitOfWorkService;
    private final CommandGateway commandGateway;


    public DeleteCourseFunctionalitySagas(SagaUnitOfWorkService unitOfWorkService, Integer courseAggregateId, SagaUnitOfWork unitOfWork, CommandGateway commandGateway) {
        this.unitOfWorkService = unitOfWorkService;
        this.commandGateway = commandGateway;
        this.buildWorkflow(courseAggregateId, unitOfWork);
    }

    public void buildWorkflow(Integer courseAggregateId, SagaUnitOfWork unitOfWork) {
        this.workflow = new SagaWorkflow(this, unitOfWorkService, unitOfWork);

        SagaStep deleteCourseStep = new SagaStep("deleteCourseStep", () -> {
            unitOfWorkService.verifySagaState(courseAggregateId, new java.util.ArrayList<SagaState>(java.util.Arrays.asList(CourseSagaState.READ_COURSE, CourseSagaState.UPDATE_COURSE, CourseSagaState.DELETE_COURSE)));
            unitOfWorkService.registerSagaState(courseAggregateId, CourseSagaState.DELETE_COURSE, unitOfWork);
            DeleteCourseCommand cmd = new DeleteCourseCommand(unitOfWork, ServiceMapping.COURSE.getServiceName(), courseAggregateId);
            commandGateway.send(cmd);
        });

        workflow.addStep(deleteCourseStep);
    }
}
