package pt.ulisboa.tecnico.socialsoftware.quizzesfull2.microservices.question.coordination.sagas;

import pt.ulisboa.tecnico.socialsoftware.ms.coordination.WorkflowFunctionality;
import pt.ulisboa.tecnico.socialsoftware.ms.messaging.CommandGateway;
import pt.ulisboa.tecnico.socialsoftware.ms.transaction.sagas.messaging.SagaCommand;
import pt.ulisboa.tecnico.socialsoftware.ms.transaction.sagas.unitOfWork.SagaUnitOfWork;
import pt.ulisboa.tecnico.socialsoftware.ms.transaction.sagas.unitOfWork.SagaUnitOfWorkService;
import pt.ulisboa.tecnico.socialsoftware.ms.transaction.sagas.workflow.SagaStep;
import pt.ulisboa.tecnico.socialsoftware.ms.transaction.sagas.workflow.SagaWorkflow;
import pt.ulisboa.tecnico.socialsoftware.quizzesfull2.ServiceMapping;
import pt.ulisboa.tecnico.socialsoftware.quizzesfull2.commands.question.DeleteQuestionCommand;
import pt.ulisboa.tecnico.socialsoftware.quizzesfull2.commands.question.GetQuestionByIdCommand;
import pt.ulisboa.tecnico.socialsoftware.quizzesfull2.microservices.question.aggregate.QuestionDto;
import pt.ulisboa.tecnico.socialsoftware.quizzesfull2.microservices.question.aggregate.sagas.states.QuestionSagaState;

import java.util.ArrayList;
import java.util.Arrays;

public class DeleteQuestionFunctionalitySagas extends WorkflowFunctionality {
    private QuestionDto questionDto;

    public DeleteQuestionFunctionalitySagas(SagaUnitOfWorkService unitOfWorkService, Integer questionAggregateId,
                                            SagaUnitOfWork unitOfWork, CommandGateway commandGateway) {
        buildWorkflow(unitOfWorkService, questionAggregateId, unitOfWork, commandGateway);
    }

    public void buildWorkflow(SagaUnitOfWorkService unitOfWorkService, Integer questionAggregateId,
                              SagaUnitOfWork unitOfWork, CommandGateway commandGateway) {
        this.workflow = new SagaWorkflow(this, unitOfWorkService, unitOfWork);

        SagaStep getQuestionStep = new SagaStep("getQuestionStep", () -> {
            GetQuestionByIdCommand readCmd = new GetQuestionByIdCommand(
                    unitOfWork, ServiceMapping.QUESTION.getServiceName(), questionAggregateId);
            SagaCommand sagaCommand = new SagaCommand(readCmd);
            sagaCommand.setSemanticLock(QuestionSagaState.IN_DELETE_QUESTION);
            this.questionDto = (QuestionDto) commandGateway.send(sagaCommand);
        });

        SagaStep deleteQuestionStep = new SagaStep("deleteQuestionStep", () -> {
            DeleteQuestionCommand cmd = new DeleteQuestionCommand(
                    unitOfWork, ServiceMapping.QUESTION.getServiceName(), questionAggregateId);
            commandGateway.send(cmd);
        }, new ArrayList<>(Arrays.asList(getQuestionStep)));

        this.workflow.addStep(getQuestionStep);
        this.workflow.addStep(deleteQuestionStep);
    }

    public QuestionDto getQuestionDto() {
        return questionDto;
    }
}
