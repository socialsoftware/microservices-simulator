package pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.tournament.coordination.sagas;

import pt.ulisboa.tecnico.socialsoftware.ms.messaging.CommandGateway;
import pt.ulisboa.tecnico.socialsoftware.ms.coordination.WorkflowFunctionality;
import pt.ulisboa.tecnico.socialsoftware.ms.transaction.sagas.unitOfWork.SagaUnitOfWork;
import pt.ulisboa.tecnico.socialsoftware.ms.transaction.sagas.unitOfWork.SagaUnitOfWorkService;
import pt.ulisboa.tecnico.socialsoftware.ms.transaction.sagas.workflow.SagaStep;
import pt.ulisboa.tecnico.socialsoftware.ms.transaction.sagas.workflow.SagaWorkflow;
import pt.ulisboa.tecnico.socialsoftware.quizzes.ServiceMapping;
import pt.ulisboa.tecnico.socialsoftware.quizzes.command.tournament.RemoveCourseExecutionCommand;

public class RemoveCourseExecutionFunctionalitySagas extends WorkflowFunctionality {
    private final SagaUnitOfWorkService unitOfWorkService;
    private final CommandGateway commandGateway;

    public RemoveCourseExecutionFunctionalitySagas(SagaUnitOfWorkService unitOfWorkService,
            Integer tournamentAggregateId, Integer courseExecutionAggregateId, Long eventVersion,
            SagaUnitOfWork unitOfWork, CommandGateway commandGateway) {
        this.unitOfWorkService = unitOfWorkService;
        this.commandGateway = commandGateway;
        this.buildWorkflow(tournamentAggregateId, courseExecutionAggregateId, eventVersion, unitOfWork);
    }

    public void buildWorkflow(Integer tournamentAggregateId, Integer courseExecutionAggregateId, Long eventVersion,
            SagaUnitOfWork unitOfWork) {
        this.workflow = new SagaWorkflow(this, unitOfWorkService, unitOfWork);

        SagaStep removeCourseExecutionStep = new SagaStep("removeCourseExecutionStep", () -> {
            RemoveCourseExecutionCommand command = new RemoveCourseExecutionCommand(unitOfWork,
                    ServiceMapping.TOURNAMENT.getServiceName(), tournamentAggregateId, courseExecutionAggregateId,
                    eventVersion);
            commandGateway.send(command);
        });

        workflow.addStep(removeCourseExecutionStep);
    }
}
