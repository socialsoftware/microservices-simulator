package pt.ulisboa.tecnico.socialsoftware.answers.microservices.quiz.coordination.sagas;

import pt.ulisboa.tecnico.socialsoftware.ms.coordination.workflow.WorkflowFunctionality;
import pt.ulisboa.tecnico.socialsoftware.ms.coordination.workflow.command.CommandGateway;
import pt.ulisboa.tecnico.socialsoftware.answers.ServiceMapping;
import pt.ulisboa.tecnico.socialsoftware.answers.command.quiz.*;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.aggregate.SagaAggregate.SagaState;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.unitOfWork.SagaUnitOfWork;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.unitOfWork.SagaUnitOfWorkService;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.workflow.SagaStep;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.workflow.SagaWorkflow;
import pt.ulisboa.tecnico.socialsoftware.answers.microservices.quiz.aggregate.sagas.states.QuizSagaState;

public class DeleteQuizFunctionalitySagas extends WorkflowFunctionality {
    private final SagaUnitOfWorkService unitOfWorkService;
    private final CommandGateway commandGateway;


    public DeleteQuizFunctionalitySagas(SagaUnitOfWorkService unitOfWorkService, Integer quizAggregateId, SagaUnitOfWork unitOfWork, CommandGateway commandGateway) {
        this.unitOfWorkService = unitOfWorkService;
        this.commandGateway = commandGateway;
        this.buildWorkflow(quizAggregateId, unitOfWork);
    }

    public void buildWorkflow(Integer quizAggregateId, SagaUnitOfWork unitOfWork) {
        this.workflow = new SagaWorkflow(this, unitOfWorkService, unitOfWork);

        SagaStep deleteQuizStep = new SagaStep("deleteQuizStep", () -> {
            unitOfWorkService.verifySagaState(quizAggregateId, new java.util.ArrayList<SagaState>(java.util.Arrays.asList(QuizSagaState.READ_QUIZ, QuizSagaState.UPDATE_QUIZ, QuizSagaState.DELETE_QUIZ)));
            unitOfWorkService.registerSagaState(quizAggregateId, QuizSagaState.DELETE_QUIZ, unitOfWork);
            DeleteQuizCommand cmd = new DeleteQuizCommand(unitOfWork, ServiceMapping.QUIZ.getServiceName(), quizAggregateId);
            commandGateway.send(cmd);
        });

        workflow.addStep(deleteQuizStep);
    }
}
