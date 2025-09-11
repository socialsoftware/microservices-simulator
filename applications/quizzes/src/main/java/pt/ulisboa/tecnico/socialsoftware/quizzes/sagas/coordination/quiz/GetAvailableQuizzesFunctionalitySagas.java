package pt.ulisboa.tecnico.socialsoftware.quizzes.sagas.coordination.quiz;

import pt.ulisboa.tecnico.socialsoftware.ms.coordination.workflow.CommandGateway;
import pt.ulisboa.tecnico.socialsoftware.ms.coordination.workflow.WorkflowFunctionality;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.unitOfWork.SagaUnitOfWork;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.unitOfWork.SagaUnitOfWorkService;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.workflow.SagaSyncStep;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.workflow.SagaWorkflow;
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.quiz.aggregate.QuizDto;
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.quiz.service.QuizService;

import java.util.List;

public class GetAvailableQuizzesFunctionalitySagas extends WorkflowFunctionality {
    private List<QuizDto> availableQuizzes;
    private final QuizService quizService;
    private final SagaUnitOfWorkService unitOfWorkService;
    private final CommandGateway commandGateway;

    public GetAvailableQuizzesFunctionalitySagas(QuizService quizService, SagaUnitOfWorkService unitOfWorkService,  
                                    Integer userAggregateId, Integer courseExecutionAggregateId, SagaUnitOfWork unitOfWork, CommandGateway commandGateway) {
        this.quizService = quizService;
        this.unitOfWorkService = unitOfWorkService;
        this.commandGateway = commandGateway;
        this.buildWorkflow(userAggregateId, courseExecutionAggregateId, unitOfWork);
    }

    public void buildWorkflow(Integer userAggregateId, Integer courseExecutionAggregateId, SagaUnitOfWork unitOfWork) {
        this.workflow = new SagaWorkflow(this, unitOfWorkService, unitOfWork);

        SagaSyncStep getAvailableQuizzesStep = new SagaSyncStep("getAvailableQuizzesStep", () -> {
            List<QuizDto> availableQuizzes = quizService.getAvailableQuizzes(courseExecutionAggregateId, unitOfWork); // TODO
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
