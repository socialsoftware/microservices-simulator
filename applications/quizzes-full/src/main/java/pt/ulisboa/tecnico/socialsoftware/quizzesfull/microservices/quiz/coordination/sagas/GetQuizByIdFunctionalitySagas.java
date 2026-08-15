package pt.ulisboa.tecnico.socialsoftware.quizzesfull.microservices.quiz.coordination.sagas;

import pt.ulisboa.tecnico.socialsoftware.ms.coordination.WorkflowFunctionality;
import pt.ulisboa.tecnico.socialsoftware.ms.messaging.CommandGateway;
import pt.ulisboa.tecnico.socialsoftware.ms.transaction.sagas.unitOfWork.SagaUnitOfWork;
import pt.ulisboa.tecnico.socialsoftware.ms.transaction.sagas.unitOfWork.SagaUnitOfWorkService;
import pt.ulisboa.tecnico.socialsoftware.ms.transaction.sagas.workflow.SagaStep;
import pt.ulisboa.tecnico.socialsoftware.ms.transaction.sagas.workflow.SagaWorkflow;
import pt.ulisboa.tecnico.socialsoftware.quizzesfull.ServiceMapping;
import pt.ulisboa.tecnico.socialsoftware.quizzesfull.commands.quiz.GetQuizByIdCommand;
import pt.ulisboa.tecnico.socialsoftware.quizzesfull.microservices.quiz.aggregate.QuizDto;

public class GetQuizByIdFunctionalitySagas extends WorkflowFunctionality {
    private QuizDto quizDto;
    private final SagaUnitOfWorkService unitOfWorkService;
    private final CommandGateway commandGateway;

    public GetQuizByIdFunctionalitySagas(SagaUnitOfWorkService unitOfWorkService,
                                          Integer quizAggregateId, SagaUnitOfWork unitOfWork,
                                          CommandGateway commandGateway) {
        this.unitOfWorkService = unitOfWorkService;
        this.commandGateway = commandGateway;
        this.buildWorkflow(quizAggregateId, unitOfWork);
    }

    public void buildWorkflow(Integer quizAggregateId, SagaUnitOfWork unitOfWork) {
        this.workflow = new SagaWorkflow(this, unitOfWorkService, unitOfWork);

        SagaStep getQuizStep = new SagaStep("getQuizStep", () -> {
            GetQuizByIdCommand cmd = new GetQuizByIdCommand(
                    unitOfWork, ServiceMapping.QUIZ.getServiceName(), quizAggregateId);
            this.quizDto = (QuizDto) commandGateway.send(cmd);
        });

        workflow.addStep(getQuizStep);
    }

    public QuizDto getQuizDto() {
        return quizDto;
    }
}
