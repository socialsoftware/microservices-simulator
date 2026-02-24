package pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.tournament.coordination.sagas;

import pt.ulisboa.tecnico.socialsoftware.ms.coordination.workflow.CommandGateway;
import pt.ulisboa.tecnico.socialsoftware.ms.coordination.workflow.WorkflowFunctionality;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.unitOfWork.SagaUnitOfWork;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.unitOfWork.SagaUnitOfWorkService;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.workflow.SagaStep;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.workflow.SagaWorkflow;
import pt.ulisboa.tecnico.socialsoftware.quizzes.ServiceMapping;
import pt.ulisboa.tecnico.socialsoftware.quizzes.command.tournament.UpdateParticipantAnswerCommand;

public class UpdateParticipantAnswerFunctionalitySagas extends WorkflowFunctionality {
    private final SagaUnitOfWorkService unitOfWorkService;
    private final CommandGateway commandGateway;

    public UpdateParticipantAnswerFunctionalitySagas(SagaUnitOfWorkService unitOfWorkService,
            Integer tournamentAggregateId,
            Integer userAggregateId, Integer executionAggregateId, Integer questionAggregateId,
            boolean correct, Integer eventVersion, SagaUnitOfWork unitOfWork,
            CommandGateway commandGateway) {
        this.unitOfWorkService = unitOfWorkService;
        this.commandGateway = commandGateway;
        buildWorkflow(tournamentAggregateId, userAggregateId, executionAggregateId, questionAggregateId, correct,
                eventVersion, unitOfWork);
    }

    private void buildWorkflow(Integer tournamentAggregateId, Integer userAggregateId, Integer executionAggregateId,
            Integer questionAggregateId, boolean correct, Integer eventVersion, SagaUnitOfWork unitOfWork) {
        this.workflow = new SagaWorkflow(this, unitOfWorkService, unitOfWork);
        SagaStep step = new SagaStep("updateParticipantAnswerStep", () -> {
            UpdateParticipantAnswerCommand command = new UpdateParticipantAnswerCommand(unitOfWork,
                    ServiceMapping.TOURNAMENT.getServiceName(),
                    tournamentAggregateId, userAggregateId, executionAggregateId, questionAggregateId, correct,
                    eventVersion);
            commandGateway.send(command);
        });
        workflow.addStep(step);
    }
}
