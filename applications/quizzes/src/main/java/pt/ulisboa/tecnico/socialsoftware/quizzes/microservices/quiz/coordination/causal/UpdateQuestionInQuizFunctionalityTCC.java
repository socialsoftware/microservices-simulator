package pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.quiz.coordination.causal;

import pt.ulisboa.tecnico.socialsoftware.ms.causal.unitOfWork.CausalUnitOfWork;
import pt.ulisboa.tecnico.socialsoftware.ms.causal.unitOfWork.CausalUnitOfWorkService;
import pt.ulisboa.tecnico.socialsoftware.ms.causal.workflow.CausalWorkflow;
import pt.ulisboa.tecnico.socialsoftware.ms.coordination.workflow.CommandGateway;
import pt.ulisboa.tecnico.socialsoftware.ms.coordination.workflow.Step;
import pt.ulisboa.tecnico.socialsoftware.ms.coordination.workflow.WorkflowFunctionality;
import pt.ulisboa.tecnico.socialsoftware.quizzes.ServiceMapping;
import pt.ulisboa.tecnico.socialsoftware.quizzes.command.quiz.UpdateQuestionCommand;

public class UpdateQuestionInQuizFunctionalityTCC extends WorkflowFunctionality {
    private final CausalUnitOfWorkService unitOfWorkService;
    private final CommandGateway commandGateway;

    public UpdateQuestionInQuizFunctionalityTCC(CausalUnitOfWorkService unitOfWorkService,
            Integer quizAggregateId, Integer questionAggregateId, String title, String content, Integer eventVersion,
            CausalUnitOfWork unitOfWork, CommandGateway commandGateway) {
        this.unitOfWorkService = unitOfWorkService;
        this.commandGateway = commandGateway;
        buildWorkflow(quizAggregateId, questionAggregateId, title, content, eventVersion, unitOfWork);
    }

    private void buildWorkflow(Integer quizAggregateId, Integer questionAggregateId, String title, String content,
            Integer eventVersion, CausalUnitOfWork unitOfWork) {
        this.workflow = new CausalWorkflow(this, unitOfWorkService, unitOfWork);
        Step step = new Step(() -> {
            UpdateQuestionCommand command = new UpdateQuestionCommand(unitOfWork, ServiceMapping.QUIZ.getServiceName(),
                    quizAggregateId, questionAggregateId, title, content, eventVersion);
            commandGateway.send(command);
        });
        workflow.addStep(step);
    }
}
