package pt.ulisboa.tecnico.socialsoftware.quizzesfull2.microservices.quiz.coordination.sagas;

import pt.ulisboa.tecnico.socialsoftware.ms.coordination.WorkflowFunctionality;
import pt.ulisboa.tecnico.socialsoftware.ms.messaging.CommandGateway;
import pt.ulisboa.tecnico.socialsoftware.ms.transaction.sagas.unitOfWork.SagaUnitOfWork;
import pt.ulisboa.tecnico.socialsoftware.ms.transaction.sagas.unitOfWork.SagaUnitOfWorkService;
import pt.ulisboa.tecnico.socialsoftware.ms.transaction.sagas.workflow.SagaStep;
import pt.ulisboa.tecnico.socialsoftware.ms.transaction.sagas.workflow.SagaWorkflow;
import pt.ulisboa.tecnico.socialsoftware.quizzesfull2.ServiceMapping;
import pt.ulisboa.tecnico.socialsoftware.quizzesfull2.commands.quiz.GetQuizzesForExecutionCommand;
import pt.ulisboa.tecnico.socialsoftware.quizzesfull2.microservices.quiz.aggregate.QuizDto;

import java.util.List;

public class GetQuizzesForExecutionFunctionalitySagas extends WorkflowFunctionality {
    private List<QuizDto> quizzes;

    public GetQuizzesForExecutionFunctionalitySagas(SagaUnitOfWorkService unitOfWorkService,
                                                    Integer executionAggregateId,
                                                    SagaUnitOfWork unitOfWork, CommandGateway commandGateway) {
        buildWorkflow(unitOfWorkService, executionAggregateId, unitOfWork, commandGateway);
    }

    @SuppressWarnings("unchecked")
    public void buildWorkflow(SagaUnitOfWorkService unitOfWorkService,
                              Integer executionAggregateId,
                              SagaUnitOfWork unitOfWork, CommandGateway commandGateway) {
        this.workflow = new SagaWorkflow(this, unitOfWorkService, unitOfWork);

        SagaStep getQuizzesForExecutionStep = new SagaStep("getQuizzesForExecutionStep", () -> {
            GetQuizzesForExecutionCommand cmd = new GetQuizzesForExecutionCommand(
                    unitOfWork, ServiceMapping.QUIZ.getServiceName(), executionAggregateId);
            this.quizzes = (List<QuizDto>) commandGateway.send(cmd);
        });

        this.workflow.addStep(getQuizzesForExecutionStep);
    }

    public List<QuizDto> getQuizzes() {
        return quizzes;
    }
}
