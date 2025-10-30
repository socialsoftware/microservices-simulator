package pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.quiz.coordination.sagas;

import pt.ulisboa.tecnico.socialsoftware.ms.coordination.workflow.CommandGateway;
import pt.ulisboa.tecnico.socialsoftware.ms.coordination.workflow.WorkflowFunctionality;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.unitOfWork.SagaUnitOfWork;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.unitOfWork.SagaUnitOfWorkService;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.workflow.SagaSyncStep;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.workflow.SagaWorkflow;
import pt.ulisboa.tecnico.socialsoftware.quizzes.ServiceMapping;
import pt.ulisboa.tecnico.socialsoftware.quizzes.command.quiz.GetAvailableQuizzesCommand;
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.quiz.aggregate.QuizDto;

import java.util.List;

public class GetAvailableQuizzesFunctionalitySagas extends WorkflowFunctionality {
    private List<QuizDto> availableQuizzes;
    private final SagaUnitOfWorkService unitOfWorkService;
    private final CommandGateway commandGateway;

    public GetAvailableQuizzesFunctionalitySagas(SagaUnitOfWorkService unitOfWorkService,
                                                 Integer userAggregateId, Integer courseExecutionAggregateId, SagaUnitOfWork unitOfWork,
                                                 CommandGateway CommandGateway) {
        this.unitOfWorkService = unitOfWorkService;
        this.commandGateway = CommandGateway;
        this.buildWorkflow(userAggregateId, courseExecutionAggregateId, unitOfWork);
    }

    public void buildWorkflow(Integer userAggregateId, Integer courseExecutionAggregateId, SagaUnitOfWork unitOfWork) {
        this.workflow = new SagaWorkflow(this, unitOfWorkService, unitOfWork);

        SagaSyncStep getAvailableQuizzesStep = new SagaSyncStep("getAvailableQuizzesStep", () -> {
            GetAvailableQuizzesCommand getAvailableQuizzesCommand = new GetAvailableQuizzesCommand(unitOfWork, ServiceMapping.QUIZ.getServiceName(), null, courseExecutionAggregateId);
            List<QuizDto> availableQuizzes = (List<QuizDto>) commandGateway.send(getAvailableQuizzesCommand);
            this.setAvailableQuizzes(availableQuizzes);
        });

        workflow.addStep(getAvailableQuizzesStep);
    }

    public List<QuizDto> getAvailableQuizzes() {
        return availableQuizzes;
    }

    public void setAvailableQuizzes(List<QuizDto> availableQuizzes) {
        this.availableQuizzes = availableQuizzes;
    }
}
