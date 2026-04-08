package pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.question.coordination.causal;

import pt.ulisboa.tecnico.socialsoftware.ms.coordination.Step;
import pt.ulisboa.tecnico.socialsoftware.ms.coordination.WorkflowFunctionality;
import pt.ulisboa.tecnico.socialsoftware.ms.messaging.CommandGateway;
import pt.ulisboa.tecnico.socialsoftware.ms.transactional.causal.unitOfWork.CausalUnitOfWork;
import pt.ulisboa.tecnico.socialsoftware.ms.transactional.causal.unitOfWork.CausalUnitOfWorkService;
import pt.ulisboa.tecnico.socialsoftware.ms.transactional.causal.workflow.CausalWorkflow;
import pt.ulisboa.tecnico.socialsoftware.quizzes.ServiceMapping;
import pt.ulisboa.tecnico.socialsoftware.quizzes.command.course.UpdateCourseQuestionCountCommand;
import pt.ulisboa.tecnico.socialsoftware.quizzes.command.question.GetQuestionByIdCommand;
import pt.ulisboa.tecnico.socialsoftware.quizzes.command.question.RemoveQuestionCommand;
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.question.aggregate.QuestionDto;
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.question.aggregate.causal.CausalQuestion;

public class RemoveQuestionFunctionalityTCC extends WorkflowFunctionality {
    private CausalQuestion question;
    private final CausalUnitOfWorkService unitOfWorkService;
    private final CommandGateway commandGateway;

    public RemoveQuestionFunctionalityTCC(CausalUnitOfWorkService unitOfWorkService,
                                          Integer questionAggregateId, CausalUnitOfWork unitOfWork, CommandGateway commandGateway) {
        this.unitOfWorkService = unitOfWorkService;
        this.commandGateway = commandGateway;
        this.buildWorkflow(questionAggregateId, unitOfWork);
    }

    public void buildWorkflow(Integer questionAggregateId, CausalUnitOfWork unitOfWork) {
        this.workflow = new CausalWorkflow(this, unitOfWorkService, unitOfWork);

        Step step = new Step(() -> {
            GetQuestionByIdCommand getCmd = new GetQuestionByIdCommand(unitOfWork, ServiceMapping.QUESTION.getServiceName(), questionAggregateId);
            QuestionDto questionDto = (QuestionDto) commandGateway.send(getCmd);
            Integer courseAggregateId = questionDto.getCourse().getAggregateId();

            RemoveQuestionCommand cmd = new RemoveQuestionCommand(unitOfWork, ServiceMapping.QUESTION.getServiceName(), questionAggregateId);
            commandGateway.send(cmd);

            // CANNOT_DELETE_LAST_EXECUTION_WITH_CONTENT
            UpdateCourseQuestionCountCommand updateCmd = new UpdateCourseQuestionCountCommand(unitOfWork, ServiceMapping.COURSE.getServiceName(), courseAggregateId, false);
            commandGateway.send(updateCmd);
        });

        workflow.addStep(step);
    }

    public CausalQuestion getQuestion() {
        return question;
    }

    public void setQuestion(CausalQuestion question) {
        this.question = question;
    }
}