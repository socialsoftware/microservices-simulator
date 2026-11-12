package pt.ulisboa.tecnico.socialsoftware.quizzesfull2.microservices.quizanswer.coordination.sagas;

import pt.ulisboa.tecnico.socialsoftware.ms.coordination.WorkflowFunctionality;
import pt.ulisboa.tecnico.socialsoftware.ms.messaging.CommandGateway;
import pt.ulisboa.tecnico.socialsoftware.ms.transaction.sagas.messaging.SagaCommand;
import pt.ulisboa.tecnico.socialsoftware.ms.transaction.sagas.unitOfWork.SagaUnitOfWork;
import pt.ulisboa.tecnico.socialsoftware.ms.transaction.sagas.unitOfWork.SagaUnitOfWorkService;
import pt.ulisboa.tecnico.socialsoftware.ms.transaction.sagas.workflow.SagaStep;
import pt.ulisboa.tecnico.socialsoftware.ms.transaction.sagas.workflow.SagaWorkflow;
import pt.ulisboa.tecnico.socialsoftware.quizzesfull2.ServiceMapping;
import pt.ulisboa.tecnico.socialsoftware.quizzesfull2.commands.quizanswer.ConcludeQuizCommand;
import pt.ulisboa.tecnico.socialsoftware.quizzesfull2.commands.quizanswer.GetQuizAnswerByIdCommand;
import pt.ulisboa.tecnico.socialsoftware.quizzesfull2.microservices.quizanswer.aggregate.QuizAnswerDto;
import pt.ulisboa.tecnico.socialsoftware.quizzesfull2.microservices.quizanswer.aggregate.sagas.states.QuizAnswerSagaState;

import java.util.ArrayList;
import java.util.Arrays;

public class ConcludeQuizFunctionalitySagas extends WorkflowFunctionality {
    private QuizAnswerDto quizAnswerDto;

    public ConcludeQuizFunctionalitySagas(SagaUnitOfWorkService unitOfWorkService, Integer quizAnswerAggregateId,
                                          SagaUnitOfWork unitOfWork, CommandGateway commandGateway) {
        buildWorkflow(unitOfWorkService, quizAnswerAggregateId, unitOfWork, commandGateway);
    }

    public void buildWorkflow(SagaUnitOfWorkService unitOfWorkService, Integer quizAnswerAggregateId,
                              SagaUnitOfWork unitOfWork, CommandGateway commandGateway) {
        this.workflow = new SagaWorkflow(this, unitOfWorkService, unitOfWork);

        SagaStep getQuizAnswerStep = new SagaStep("getQuizAnswerStep", () -> {
            GetQuizAnswerByIdCommand readCmd = new GetQuizAnswerByIdCommand(
                    unitOfWork, ServiceMapping.QUIZ_ANSWER.getServiceName(), quizAnswerAggregateId);
            SagaCommand sagaCommand = new SagaCommand(readCmd);
            sagaCommand.setSemanticLock(QuizAnswerSagaState.IN_CONCLUDE_QUIZ);
            this.quizAnswerDto = (QuizAnswerDto) commandGateway.send(sagaCommand);
        });

        SagaStep concludeQuizStep = new SagaStep("concludeQuizStep", () -> {
            ConcludeQuizCommand cmd = new ConcludeQuizCommand(
                    unitOfWork, ServiceMapping.QUIZ_ANSWER.getServiceName(), quizAnswerAggregateId);
            commandGateway.send(cmd);
        }, new ArrayList<>(Arrays.asList(getQuizAnswerStep)));

        this.workflow.addStep(getQuizAnswerStep);
        this.workflow.addStep(concludeQuizStep);
    }

    public QuizAnswerDto getQuizAnswerDto() {
        return quizAnswerDto;
    }
}
