package pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.answer.coordination.causal;

import pt.ulisboa.tecnico.socialsoftware.ms.coordination.Step;
import pt.ulisboa.tecnico.socialsoftware.ms.coordination.WorkflowFunctionality;
import pt.ulisboa.tecnico.socialsoftware.ms.messaging.CommandGateway;
import pt.ulisboa.tecnico.socialsoftware.ms.transactional.causal.unitOfWork.CausalUnitOfWork;
import pt.ulisboa.tecnico.socialsoftware.ms.transactional.causal.unitOfWork.CausalUnitOfWorkService;
import pt.ulisboa.tecnico.socialsoftware.ms.transactional.causal.workflow.CausalWorkflow;
import pt.ulisboa.tecnico.socialsoftware.quizzes.ServiceMapping;
import pt.ulisboa.tecnico.socialsoftware.quizzes.commands.answer.ConcludeQuizCommand;
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.answer.aggregate.causal.CausalQuizAnswer;

public class ConcludeQuizFunctionalityTCC extends WorkflowFunctionality {
    private CausalQuizAnswer quizAnswer;
    private final CausalUnitOfWorkService unitOfWorkService;
    private final CommandGateway commandGateway;

    public ConcludeQuizFunctionalityTCC(CausalUnitOfWorkService unitOfWorkService,
                                        Integer quizAggregateId, Integer userAggregateId, CausalUnitOfWork unitOfWork,
                                        CommandGateway commandGateway) {
        this.unitOfWorkService = unitOfWorkService;
        this.commandGateway = commandGateway;
        this.buildWorkflow(quizAggregateId, userAggregateId, unitOfWork);
    }

    public void buildWorkflow(Integer quizAggregateId, Integer userAggregateId, CausalUnitOfWork unitOfWork) {
        this.workflow = new CausalWorkflow(this, unitOfWorkService, unitOfWork);

        Step step = new Step(() -> {
            ConcludeQuizCommand concludeQuizCommand = new ConcludeQuizCommand(unitOfWork,
                    ServiceMapping.QUIZ.getServiceName(), quizAggregateId, userAggregateId);
            commandGateway.send(concludeQuizCommand);
        });

        workflow.addStep(step);
    }

    public CausalQuizAnswer getQuizAnswer() {
        return quizAnswer;
    }

    public void setQuizAnswer(CausalQuizAnswer quizAnswer) {
        this.quizAnswer = quizAnswer;
    }
}