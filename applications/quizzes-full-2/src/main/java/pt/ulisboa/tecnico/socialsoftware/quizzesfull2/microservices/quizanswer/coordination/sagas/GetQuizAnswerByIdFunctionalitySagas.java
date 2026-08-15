package pt.ulisboa.tecnico.socialsoftware.quizzesfull2.microservices.quizanswer.coordination.sagas;

import pt.ulisboa.tecnico.socialsoftware.ms.coordination.WorkflowFunctionality;
import pt.ulisboa.tecnico.socialsoftware.ms.messaging.CommandGateway;
import pt.ulisboa.tecnico.socialsoftware.ms.transaction.sagas.unitOfWork.SagaUnitOfWork;
import pt.ulisboa.tecnico.socialsoftware.ms.transaction.sagas.unitOfWork.SagaUnitOfWorkService;
import pt.ulisboa.tecnico.socialsoftware.ms.transaction.sagas.workflow.SagaStep;
import pt.ulisboa.tecnico.socialsoftware.ms.transaction.sagas.workflow.SagaWorkflow;
import pt.ulisboa.tecnico.socialsoftware.quizzesfull2.ServiceMapping;
import pt.ulisboa.tecnico.socialsoftware.quizzesfull2.commands.quizanswer.GetQuizAnswerByIdCommand;
import pt.ulisboa.tecnico.socialsoftware.quizzesfull2.microservices.quizanswer.aggregate.QuizAnswerDto;

public class GetQuizAnswerByIdFunctionalitySagas extends WorkflowFunctionality {
    private QuizAnswerDto quizAnswerDto;

    public GetQuizAnswerByIdFunctionalitySagas(SagaUnitOfWorkService unitOfWorkService,
                                               Integer quizAnswerAggregateId,
                                               SagaUnitOfWork unitOfWork, CommandGateway commandGateway) {
        buildWorkflow(unitOfWorkService, quizAnswerAggregateId, unitOfWork, commandGateway);
    }

    public void buildWorkflow(SagaUnitOfWorkService unitOfWorkService,
                              Integer quizAnswerAggregateId,
                              SagaUnitOfWork unitOfWork, CommandGateway commandGateway) {
        this.workflow = new SagaWorkflow(this, unitOfWorkService, unitOfWork);

        SagaStep getQuizAnswerStep = new SagaStep("getQuizAnswerStep", () -> {
            GetQuizAnswerByIdCommand cmd = new GetQuizAnswerByIdCommand(
                    unitOfWork, ServiceMapping.QUIZ_ANSWER.getServiceName(), quizAnswerAggregateId);
            this.quizAnswerDto = (QuizAnswerDto) commandGateway.send(cmd);
        });

        this.workflow.addStep(getQuizAnswerStep);
    }

    public QuizAnswerDto getQuizAnswerDto() {
        return quizAnswerDto;
    }
}
