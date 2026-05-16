package pt.ulisboa.tecnico.socialsoftware.quizzesfull.microservices.quizanswer.coordination.sagas;

import pt.ulisboa.tecnico.socialsoftware.ms.coordination.WorkflowFunctionality;
import pt.ulisboa.tecnico.socialsoftware.ms.messaging.Command;
import pt.ulisboa.tecnico.socialsoftware.ms.messaging.CommandGateway;
import pt.ulisboa.tecnico.socialsoftware.ms.transaction.sagas.aggregate.GenericSagaState;
import pt.ulisboa.tecnico.socialsoftware.ms.transaction.sagas.messaging.SagaCommand;
import pt.ulisboa.tecnico.socialsoftware.ms.transaction.sagas.unitOfWork.SagaUnitOfWork;
import pt.ulisboa.tecnico.socialsoftware.ms.transaction.sagas.unitOfWork.SagaUnitOfWorkService;
import pt.ulisboa.tecnico.socialsoftware.ms.transaction.sagas.workflow.SagaStep;
import pt.ulisboa.tecnico.socialsoftware.ms.transaction.sagas.workflow.SagaWorkflow;
import pt.ulisboa.tecnico.socialsoftware.quizzesfull.ServiceMapping;
import pt.ulisboa.tecnico.socialsoftware.quizzesfull.commands.quizanswer.ConcludeQuizCommand;
import pt.ulisboa.tecnico.socialsoftware.quizzesfull.commands.quizanswer.GetQuizAnswerByIdCommand;
import pt.ulisboa.tecnico.socialsoftware.quizzesfull.microservices.quizanswer.aggregate.sagas.states.QuizAnswerSagaState;

import java.util.ArrayList;
import java.util.Arrays;

public class ConcludeQuizFunctionalitySagas extends WorkflowFunctionality {
    private final SagaUnitOfWorkService unitOfWorkService;
    private final CommandGateway commandGateway;

    public ConcludeQuizFunctionalitySagas(SagaUnitOfWorkService unitOfWorkService,
                                           Integer quizAnswerId,
                                           SagaUnitOfWork unitOfWork, CommandGateway commandGateway) {
        this.unitOfWorkService = unitOfWorkService;
        this.commandGateway = commandGateway;
        this.buildWorkflow(quizAnswerId, unitOfWork);
    }

    public void buildWorkflow(Integer quizAnswerId, SagaUnitOfWork unitOfWork) {
        this.workflow = new SagaWorkflow(this, unitOfWorkService, unitOfWork);

        SagaStep getQuizAnswerStep = new SagaStep("getQuizAnswerStep", () -> {
            GetQuizAnswerByIdCommand getCmd = new GetQuizAnswerByIdCommand(
                    unitOfWork, ServiceMapping.ANSWER.getServiceName(), quizAnswerId);
            SagaCommand sagaCmd = new SagaCommand(getCmd);
            sagaCmd.setSemanticLock(QuizAnswerSagaState.IN_CONCLUDE_QUIZ);
            commandGateway.send(sagaCmd);
        });

        getQuizAnswerStep.registerCompensation(() -> {
            Command releaseCmd = new Command(unitOfWork, ServiceMapping.ANSWER.getServiceName(), quizAnswerId);
            SagaCommand release = new SagaCommand(releaseCmd);
            release.setSemanticLock(GenericSagaState.NOT_IN_SAGA);
            commandGateway.send(release);
        }, unitOfWork);

        SagaStep concludeQuizStep = new SagaStep("concludeQuizStep", () -> {
            ConcludeQuizCommand cmd = new ConcludeQuizCommand(
                    unitOfWork, ServiceMapping.ANSWER.getServiceName(), quizAnswerId);
            commandGateway.send(cmd);
        }, new ArrayList<>(Arrays.asList(getQuizAnswerStep)));

        this.workflow.addStep(getQuizAnswerStep);
        this.workflow.addStep(concludeQuizStep);
    }
}
