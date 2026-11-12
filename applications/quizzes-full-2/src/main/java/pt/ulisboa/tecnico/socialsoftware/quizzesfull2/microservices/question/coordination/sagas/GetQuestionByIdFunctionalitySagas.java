package pt.ulisboa.tecnico.socialsoftware.quizzesfull2.microservices.question.coordination.sagas;

import pt.ulisboa.tecnico.socialsoftware.ms.coordination.WorkflowFunctionality;
import pt.ulisboa.tecnico.socialsoftware.ms.messaging.CommandGateway;
import pt.ulisboa.tecnico.socialsoftware.ms.transaction.sagas.unitOfWork.SagaUnitOfWork;
import pt.ulisboa.tecnico.socialsoftware.ms.transaction.sagas.unitOfWork.SagaUnitOfWorkService;
import pt.ulisboa.tecnico.socialsoftware.ms.transaction.sagas.workflow.SagaStep;
import pt.ulisboa.tecnico.socialsoftware.ms.transaction.sagas.workflow.SagaWorkflow;
import pt.ulisboa.tecnico.socialsoftware.quizzesfull2.ServiceMapping;
import pt.ulisboa.tecnico.socialsoftware.quizzesfull2.commands.question.GetQuestionByIdCommand;
import pt.ulisboa.tecnico.socialsoftware.quizzesfull2.microservices.question.aggregate.QuestionDto;

public class GetQuestionByIdFunctionalitySagas extends WorkflowFunctionality {
    private QuestionDto questionDto;

    public GetQuestionByIdFunctionalitySagas(SagaUnitOfWorkService unitOfWorkService,
                                             Integer questionAggregateId,
                                             SagaUnitOfWork unitOfWork, CommandGateway commandGateway) {
        buildWorkflow(unitOfWorkService, questionAggregateId, unitOfWork, commandGateway);
    }

    public void buildWorkflow(SagaUnitOfWorkService unitOfWorkService,
                              Integer questionAggregateId,
                              SagaUnitOfWork unitOfWork, CommandGateway commandGateway) {
        this.workflow = new SagaWorkflow(this, unitOfWorkService, unitOfWork);

        SagaStep getQuestionStep = new SagaStep("getQuestionStep", () -> {
            GetQuestionByIdCommand cmd = new GetQuestionByIdCommand(
                    unitOfWork, ServiceMapping.QUESTION.getServiceName(), questionAggregateId);
            this.questionDto = (QuestionDto) commandGateway.send(cmd);
        });

        this.workflow.addStep(getQuestionStep);
    }

    public QuestionDto getQuestionDto() {
        return questionDto;
    }
}
