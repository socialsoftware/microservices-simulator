package pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.question.coordination.causal;

import pt.ulisboa.tecnico.socialsoftware.ms.causal.unitOfWork.CausalUnitOfWork;
import pt.ulisboa.tecnico.socialsoftware.ms.causal.unitOfWork.CausalUnitOfWorkService;
import pt.ulisboa.tecnico.socialsoftware.ms.causal.workflow.CausalWorkflow;
import pt.ulisboa.tecnico.socialsoftware.ms.coordination.workflow.CommandGateway;
import pt.ulisboa.tecnico.socialsoftware.ms.coordination.workflow.SyncStep;
import pt.ulisboa.tecnico.socialsoftware.ms.coordination.workflow.WorkflowFunctionality;
import pt.ulisboa.tecnico.socialsoftware.quizzes.ServiceMapping;
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.question.aggregate.causal.CausalQuestion;
import pt.ulisboa.tecnico.socialsoftware.quizzes.command.question.RemoveQuestionCommand;

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

        SyncStep step = new SyncStep(() -> {
            RemoveQuestionCommand cmd = new RemoveQuestionCommand(unitOfWork, ServiceMapping.QUESTION.getServiceName(), questionAggregateId);
            commandGateway.send(cmd);
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