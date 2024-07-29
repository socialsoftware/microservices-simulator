package pt.ulisboa.tecnico.socialsoftware.ms.quizzes.sagas.coordination.functionalitiesWorkflows;

import java.util.List;

import pt.ulisboa.tecnico.socialsoftware.ms.coordination.workflow.SyncStep;
import pt.ulisboa.tecnico.socialsoftware.ms.coordination.workflow.WorkflowFunctionality;
import pt.ulisboa.tecnico.socialsoftware.ms.quizzes.microservices.quiz.aggregate.QuizDto;
import pt.ulisboa.tecnico.socialsoftware.ms.quizzes.microservices.quiz.service.QuizService;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.unityOfWork.SagaUnitOfWork;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.unityOfWork.SagaUnitOfWorkService;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.workflow.SagaWorkflow;

public class GetAvailableQuizzesFunctionality extends WorkflowFunctionality {
    private List<QuizDto> availableQuizzes;

    private SagaWorkflow workflow;

    private final QuizService quizService;
    private final SagaUnitOfWorkService unitOfWorkService;

    public GetAvailableQuizzesFunctionality(QuizService quizService, SagaUnitOfWorkService unitOfWorkService,  
                                    Integer userAggregateId, Integer courseExecutionAggregateId, SagaUnitOfWork unitOfWork) {
        this.quizService = quizService;
        this.unitOfWorkService = unitOfWorkService;
        this.buildWorkflow(userAggregateId, courseExecutionAggregateId, unitOfWork);
    }

    public void buildWorkflow(Integer userAggregateId, Integer courseExecutionAggregateId, SagaUnitOfWork unitOfWork) {
        this.workflow = new SagaWorkflow(this, unitOfWorkService, unitOfWork);

        SyncStep getAvailableQuizzesStep = new SyncStep(() -> {
            List<QuizDto> availableQuizzes = quizService.getAvailableQuizzes(courseExecutionAggregateId, unitOfWork);
            this.setAvailableQuizzes(availableQuizzes);
        });
    
        workflow.addStep(getAvailableQuizzesStep);
    }

    @Override
    public void handleEvents() {

    }

    public void executeWorkflow(SagaUnitOfWork unitOfWork) {
        workflow.execute(unitOfWork);
    }

    public void executeStepByName(String stepName, SagaUnitOfWork unitOfWork) {
        workflow.executeStepByName(stepName, unitOfWork);
    }

    public void executeUntilStep(String stepName, SagaUnitOfWork unitOfWork) {
        workflow.executeUntilStep(stepName, unitOfWork);
    }

    public void resumeWorkflow(SagaUnitOfWork unitOfWork) {
        workflow.resume(unitOfWork);
    }

    public List<QuizDto> getAvailableQuizzes() {
        return availableQuizzes;
    }

    public void setAvailableQuizzes(List<QuizDto> availableQuizzes) {
        this.availableQuizzes = availableQuizzes;
    }
}
