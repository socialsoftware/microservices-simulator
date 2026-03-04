package pt.ulisboa.tecnico.socialsoftware.answers.microservices.course.coordination.sagas;

import pt.ulisboa.tecnico.socialsoftware.ms.coordination.workflow.WorkflowFunctionality;
import pt.ulisboa.tecnico.socialsoftware.ms.coordination.workflow.CommandGateway;
import pt.ulisboa.tecnico.socialsoftware.answers.ServiceMapping;
import pt.ulisboa.tecnico.socialsoftware.answers.command.course.*;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.unitOfWork.SagaUnitOfWork;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.unitOfWork.SagaUnitOfWorkService;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.workflow.SagaStep;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.workflow.SagaWorkflow;

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
            DeleteCourseCommand cmd = new DeleteCourseCommand(unitOfWork, ServiceMapping.COURSE.getServiceName(), courseAggregateId);
            commandGateway.send(cmd);
        });

        workflow.addStep(deleteCourseStep);
    }
}
