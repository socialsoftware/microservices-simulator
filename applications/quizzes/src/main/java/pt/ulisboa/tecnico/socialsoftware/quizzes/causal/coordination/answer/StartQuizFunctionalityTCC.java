package pt.ulisboa.tecnico.socialsoftware.quizzes.causal.coordination.answer;

import pt.ulisboa.tecnico.socialsoftware.ms.causal.unityOfWork.CausalUnitOfWork;
import pt.ulisboa.tecnico.socialsoftware.ms.causal.unityOfWork.CausalUnitOfWorkService;
import pt.ulisboa.tecnico.socialsoftware.ms.causal.workflow.CausalWorkflow;
import pt.ulisboa.tecnico.socialsoftware.ms.coordination.workflow.SyncStep;
import pt.ulisboa.tecnico.socialsoftware.ms.coordination.workflow.WorkflowFunctionality;
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.answer.service.QuizAnswerService;
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.quiz.aggregate.QuizDto;
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.quiz.service.QuizService;

public class StartQuizFunctionalityTCC extends WorkflowFunctionality {
    private QuizDto quizDto;
    private final QuizAnswerService quizAnswerService;
    private final CausalUnitOfWorkService unitOfWorkService;

    public StartQuizFunctionalityTCC(QuizAnswerService quizAnswerService, QuizService quizService, CausalUnitOfWorkService unitOfWorkService,  
                        Integer quizAggregateId, Integer courseExecutionAggregateId, Integer userAggregateId, CausalUnitOfWork unitOfWork) {
        this.quizAnswerService = quizAnswerService;
        this.unitOfWorkService = unitOfWorkService;
        this.buildWorkflow(quizAggregateId, courseExecutionAggregateId, userAggregateId, unitOfWork);
    }

    public void buildWorkflow(Integer quizAggregateId, Integer courseExecutionAggregateId, Integer userAggregateId, CausalUnitOfWork unitOfWork) {
        this.workflow = new CausalWorkflow(this, unitOfWorkService, unitOfWork);

        SyncStep step = new SyncStep(() -> {
            quizAnswerService.startQuiz(quizAggregateId, courseExecutionAggregateId, userAggregateId, unitOfWork);
        });
    
        workflow.addStep(step);
    }
    

    public QuizDto getQuizDto() {
        return quizDto;
    }

    public void setQuizDto(QuizDto quizDto) {
        this.quizDto = quizDto;
    }
}