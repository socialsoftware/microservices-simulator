package pt.ulisboa.tecnico.socialsoftware.crossrefs.microservices.enrollment.coordination.sagas;

import pt.ulisboa.tecnico.socialsoftware.ms.coordination.workflow.WorkflowFunctionality;
import pt.ulisboa.tecnico.socialsoftware.ms.coordination.workflow.command.CommandGateway;
import pt.ulisboa.tecnico.socialsoftware.crossrefs.ServiceMapping;
import pt.ulisboa.tecnico.socialsoftware.crossrefs.command.enrollment.*;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.unitOfWork.SagaUnitOfWork;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.unitOfWork.SagaUnitOfWorkService;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.workflow.SagaStep;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.workflow.SagaWorkflow;

public class DeleteEnrollmentFunctionalitySagas extends WorkflowFunctionality {
    private final SagaUnitOfWorkService unitOfWorkService;
    private final CommandGateway commandGateway;


    public DeleteEnrollmentFunctionalitySagas(SagaUnitOfWorkService unitOfWorkService, Integer enrollmentAggregateId, SagaUnitOfWork unitOfWork, CommandGateway commandGateway) {
        this.unitOfWorkService = unitOfWorkService;
        this.commandGateway = commandGateway;
        this.buildWorkflow(enrollmentAggregateId, unitOfWork);
    }

    public void buildWorkflow(Integer enrollmentAggregateId, SagaUnitOfWork unitOfWork) {
        this.workflow = new SagaWorkflow(this, unitOfWorkService, unitOfWork);

        SagaStep deleteEnrollmentStep = new SagaStep("deleteEnrollmentStep", () -> {
            DeleteEnrollmentCommand cmd = new DeleteEnrollmentCommand(unitOfWork, ServiceMapping.ENROLLMENT.getServiceName(), enrollmentAggregateId);
            commandGateway.send(cmd);
        });

        workflow.addStep(deleteEnrollmentStep);
    }
}
