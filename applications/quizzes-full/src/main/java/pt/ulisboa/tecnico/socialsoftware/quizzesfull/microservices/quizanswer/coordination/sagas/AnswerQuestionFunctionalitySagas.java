package pt.ulisboa.tecnico.socialsoftware.quizzesfull.microservices.quizanswer.coordination.sagas;

import pt.ulisboa.tecnico.socialsoftware.ms.coordination.WorkflowFunctionality;
import pt.ulisboa.tecnico.socialsoftware.ms.messaging.CommandGateway;
import pt.ulisboa.tecnico.socialsoftware.ms.transaction.sagas.messaging.SagaCommand;
import pt.ulisboa.tecnico.socialsoftware.ms.transaction.sagas.unitOfWork.SagaUnitOfWork;
import pt.ulisboa.tecnico.socialsoftware.ms.transaction.sagas.unitOfWork.SagaUnitOfWorkService;
import pt.ulisboa.tecnico.socialsoftware.ms.transaction.sagas.workflow.SagaStep;
import pt.ulisboa.tecnico.socialsoftware.ms.transaction.sagas.workflow.SagaWorkflow;
import pt.ulisboa.tecnico.socialsoftware.quizzesfull.ServiceMapping;
import pt.ulisboa.tecnico.socialsoftware.quizzesfull.commands.question.GetQuestionByIdCommand;
import pt.ulisboa.tecnico.socialsoftware.quizzesfull.commands.quizanswer.AnswerQuestionCommand;
import pt.ulisboa.tecnico.socialsoftware.quizzesfull.commands.quizanswer.GetQuizAnswerByIdCommand;
import pt.ulisboa.tecnico.socialsoftware.quizzesfull.microservices.question.aggregate.QuestionDto;
import pt.ulisboa.tecnico.socialsoftware.quizzesfull.microservices.quizanswer.aggregate.sagas.states.QuizAnswerSagaState;

import java.util.ArrayList;
import java.util.Arrays;

public class AnswerQuestionFunctionalitySagas extends WorkflowFunctionality {
    private QuestionDto questionDto;
    private final SagaUnitOfWorkService unitOfWorkService;
    private final CommandGateway commandGateway;

    public AnswerQuestionFunctionalitySagas(SagaUnitOfWorkService unitOfWorkService,
                                             Integer quizAnswerId, Integer questionId,
                                             Integer optionKey, Integer timeTaken,
                                             SagaUnitOfWork unitOfWork, CommandGateway commandGateway) {
        this.unitOfWorkService = unitOfWorkService;
        this.commandGateway = commandGateway;
        this.buildWorkflow(quizAnswerId, questionId, optionKey, timeTaken, unitOfWork);
    }

    public void buildWorkflow(Integer quizAnswerId, Integer questionId,
                               Integer optionKey, Integer timeTaken,
                               SagaUnitOfWork unitOfWork) {
        this.workflow = new SagaWorkflow(this, unitOfWorkService, unitOfWork);

        SagaStep getQuizAnswerStep = new SagaStep("getQuizAnswerStep", () -> {
            GetQuizAnswerByIdCommand getCmd = new GetQuizAnswerByIdCommand(
                    unitOfWork, ServiceMapping.ANSWER.getServiceName(), quizAnswerId);
            SagaCommand sagaCmd = new SagaCommand(getCmd);
            sagaCmd.setSemanticLock(QuizAnswerSagaState.IN_ANSWER_QUESTION);
            commandGateway.send(sagaCmd);
        });

        SagaStep getQuestionStep = new SagaStep("getQuestionStep", () -> {
            GetQuestionByIdCommand getCmd = new GetQuestionByIdCommand(
                    unitOfWork, ServiceMapping.QUESTION.getServiceName(), questionId);
            this.questionDto = (QuestionDto) commandGateway.send(getCmd);
        }, new ArrayList<>(Arrays.asList(getQuizAnswerStep)));

        SagaStep answerQuestionStep = new SagaStep("answerQuestionStep", () -> {
            AnswerQuestionCommand cmd = new AnswerQuestionCommand(
                    unitOfWork, ServiceMapping.ANSWER.getServiceName(),
                    quizAnswerId, questionId, this.questionDto.getVersion(), optionKey, timeTaken);
            commandGateway.send(cmd);
        }, new ArrayList<>(Arrays.asList(getQuestionStep)));

        this.workflow.addStep(getQuizAnswerStep);
        this.workflow.addStep(getQuestionStep);
        this.workflow.addStep(answerQuestionStep);
    }
}
