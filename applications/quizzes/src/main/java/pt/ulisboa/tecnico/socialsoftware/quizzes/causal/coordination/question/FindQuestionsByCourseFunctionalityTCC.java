package pt.ulisboa.tecnico.socialsoftware.quizzes.causal.coordination.question;

import java.util.List;

import pt.ulisboa.tecnico.socialsoftware.ms.causal.unityOfWork.CausalUnitOfWork;
import pt.ulisboa.tecnico.socialsoftware.ms.causal.unityOfWork.CausalUnitOfWorkService;
import pt.ulisboa.tecnico.socialsoftware.ms.causal.workflow.CausalWorkflow;
import pt.ulisboa.tecnico.socialsoftware.ms.coordination.workflow.SyncStep;
import pt.ulisboa.tecnico.socialsoftware.ms.coordination.workflow.WorkflowFunctionality;
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.question.aggregate.QuestionDto;
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.question.service.QuestionService;

public class FindQuestionsByCourseFunctionalityTCC extends WorkflowFunctionality {
    private List<QuestionDto> questions;
    private final QuestionService questionService;
    private final CausalUnitOfWorkService unitOfWorkService;

    public FindQuestionsByCourseFunctionalityTCC(QuestionService questionService, CausalUnitOfWorkService unitOfWorkService,  
                            Integer courseAggregateId, CausalUnitOfWork unitOfWork) {
        this.questionService = questionService;
        this.unitOfWorkService = unitOfWorkService;
        this.buildWorkflow(courseAggregateId, unitOfWork);
    }

    public void buildWorkflow(Integer courseAggregateId, CausalUnitOfWork unitOfWork) {
        this.workflow = new CausalWorkflow(this, unitOfWorkService, unitOfWork);

        SyncStep step = new SyncStep(() -> {
            this.questions = questionService.findQuestionsByCourseAggregateId(courseAggregateId, unitOfWork);
        });
    
        workflow.addStep(step);
    }
    

    public List<QuestionDto> getQuestions() {
        return questions;
    }

    public void setQuestions(List<QuestionDto> questions) {
        this.questions = questions;
    }
}
