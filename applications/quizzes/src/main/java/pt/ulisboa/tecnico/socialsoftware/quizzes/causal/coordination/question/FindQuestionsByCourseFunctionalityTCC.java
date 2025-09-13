package pt.ulisboa.tecnico.socialsoftware.quizzes.causal.coordination.question;

import pt.ulisboa.tecnico.socialsoftware.ms.causal.unitOfWork.CausalUnitOfWork;
import pt.ulisboa.tecnico.socialsoftware.ms.causal.unitOfWork.CausalUnitOfWorkService;
import pt.ulisboa.tecnico.socialsoftware.ms.causal.workflow.CausalWorkflow;
import pt.ulisboa.tecnico.socialsoftware.ms.coordination.workflow.CommandGateway;
import pt.ulisboa.tecnico.socialsoftware.ms.coordination.workflow.SyncStep;
import pt.ulisboa.tecnico.socialsoftware.ms.coordination.workflow.WorkflowFunctionality;
import pt.ulisboa.tecnico.socialsoftware.quizzes.ServiceMapping;
import pt.ulisboa.tecnico.socialsoftware.quizzes.command.question.FindQuestionsByCourseAggregateIdCommand;
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.question.aggregate.QuestionDto;
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.question.service.QuestionService;

import java.util.List;

public class FindQuestionsByCourseFunctionalityTCC extends WorkflowFunctionality {
    private List<QuestionDto> questions;
    private final QuestionService questionService;
    private final CausalUnitOfWorkService unitOfWorkService;
    private final CommandGateway commandGateway;

    public FindQuestionsByCourseFunctionalityTCC(QuestionService questionService, CausalUnitOfWorkService unitOfWorkService,  
                            Integer courseAggregateId, CausalUnitOfWork unitOfWork, CommandGateway commandGateway) {
        this.questionService = questionService;
        this.unitOfWorkService = unitOfWorkService;
        this.commandGateway = commandGateway;
        this.buildWorkflow(courseAggregateId, unitOfWork);
    }

    public void buildWorkflow(Integer courseAggregateId, CausalUnitOfWork unitOfWork) {
        this.workflow = new CausalWorkflow(this, unitOfWorkService, unitOfWork);

        SyncStep step = new SyncStep(() -> {
//            this.questions = questionService.findQuestionsByCourseAggregateId(courseAggregateId, unitOfWork);
            FindQuestionsByCourseAggregateIdCommand cmd = new FindQuestionsByCourseAggregateIdCommand(unitOfWork, ServiceMapping.QUESTION.getServiceName(), courseAggregateId);
            List<QuestionDto> result = (List<QuestionDto>) commandGateway.send(cmd);
            this.questions = result;
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
