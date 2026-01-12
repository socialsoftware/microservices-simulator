package pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.quiz.coordination.sagas;

import pt.ulisboa.tecnico.socialsoftware.ms.coordination.workflow.CommandGateway;
import pt.ulisboa.tecnico.socialsoftware.ms.coordination.workflow.WorkflowFunctionality;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.unitOfWork.SagaUnitOfWork;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.unitOfWork.SagaUnitOfWorkService;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.workflow.SagaStep;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.workflow.SagaWorkflow;
import pt.ulisboa.tecnico.socialsoftware.quizzes.ServiceMapping;
import pt.ulisboa.tecnico.socialsoftware.quizzes.command.quiz.GetQuizByIdCommand;
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.quiz.aggregate.QuizDto;

public class FindQuizFunctionalitySagas extends WorkflowFunctionality {
    private QuizDto quizDto;
    private final SagaUnitOfWorkService unitOfWorkService;
    private final CommandGateway commandGateway;

    public FindQuizFunctionalitySagas(SagaUnitOfWorkService unitOfWorkService,
                                      Integer quizAggregateId, SagaUnitOfWork unitOfWork, CommandGateway CommandGateway) {
        this.unitOfWorkService = unitOfWorkService;
        this.commandGateway = CommandGateway;
        this.buildWorkflow(quizAggregateId, unitOfWork);
    }

    public void buildWorkflow(Integer quizAggregateId, SagaUnitOfWork unitOfWork) {
        this.workflow = new SagaWorkflow(this, unitOfWorkService, unitOfWork);

        SagaStep findQuizStep = new SagaStep("findQuizStep", () -> {
            GetQuizByIdCommand getQuizByIdCommand = new GetQuizByIdCommand(unitOfWork, ServiceMapping.QUIZ.getServiceName(), quizAggregateId);
            QuizDto quizDto = (QuizDto) commandGateway.send(getQuizByIdCommand);
            this.setQuizDto(quizDto);
        });

        workflow.addStep(findQuizStep);
    }

    public QuizDto getQuizDto() {
        return quizDto;
    }

    public void setQuizDto(QuizDto quizDto) {
        this.quizDto = quizDto;
    }
}