package pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.question.coordination.causal;

import pt.ulisboa.tecnico.socialsoftware.ms.causal.unitOfWork.CausalUnitOfWork;
import pt.ulisboa.tecnico.socialsoftware.ms.causal.unitOfWork.CausalUnitOfWorkService;
import pt.ulisboa.tecnico.socialsoftware.ms.causal.workflow.CausalWorkflow;
import pt.ulisboa.tecnico.socialsoftware.ms.coordination.workflow.CommandGateway;
import pt.ulisboa.tecnico.socialsoftware.ms.coordination.workflow.Step;
import pt.ulisboa.tecnico.socialsoftware.ms.coordination.workflow.WorkflowFunctionality;
import pt.ulisboa.tecnico.socialsoftware.quizzes.ServiceMapping;
import pt.ulisboa.tecnico.socialsoftware.quizzes.command.question.FindQuestionsByCourseAggregateIdCommand;
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.question.aggregate.QuestionDto;

import java.util.List;

public class FindQuestionsByCourseFunctionalityTCC extends WorkflowFunctionality {
    private List<QuestionDto> questions;
    private final CausalUnitOfWorkService unitOfWorkService;
    private final CommandGateway commandGateway;

    public FindQuestionsByCourseFunctionalityTCC(CausalUnitOfWorkService unitOfWorkService,
                                                 Integer courseAggregateId, CausalUnitOfWork unitOfWork, CommandGateway commandGateway) {
        this.unitOfWorkService = unitOfWorkService;
        this.commandGateway = commandGateway;
        this.buildWorkflow(courseAggregateId, unitOfWork);
    }

    public void buildWorkflow(Integer courseAggregateId, CausalUnitOfWork unitOfWork) {
        this.workflow = new CausalWorkflow(this, unitOfWorkService, unitOfWork);

        Step step = new Step(() -> {
            FindQuestionsByCourseAggregateIdCommand cmd = new FindQuestionsByCourseAggregateIdCommand(unitOfWork,
                    ServiceMapping.QUESTION.getServiceName(), courseAggregateId);
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
