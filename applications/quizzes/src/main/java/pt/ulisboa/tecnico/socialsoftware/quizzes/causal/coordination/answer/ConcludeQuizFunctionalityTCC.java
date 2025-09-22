package pt.ulisboa.tecnico.socialsoftware.quizzes.causal.coordination.answer;

import pt.ulisboa.tecnico.socialsoftware.ms.causal.unitOfWork.CausalUnitOfWork;
import pt.ulisboa.tecnico.socialsoftware.ms.causal.unitOfWork.CausalUnitOfWorkService;
import pt.ulisboa.tecnico.socialsoftware.ms.causal.workflow.CausalWorkflow;
import pt.ulisboa.tecnico.socialsoftware.ms.coordination.workflow.CommandGateway;
import pt.ulisboa.tecnico.socialsoftware.ms.coordination.workflow.SyncStep;
import pt.ulisboa.tecnico.socialsoftware.ms.coordination.workflow.WorkflowFunctionality;
import pt.ulisboa.tecnico.socialsoftware.quizzes.ServiceMapping;
import pt.ulisboa.tecnico.socialsoftware.quizzes.causal.aggregates.CausalQuizAnswer;
import pt.ulisboa.tecnico.socialsoftware.quizzes.command.answer.ConcludeQuizCommand;
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.answer.service.QuizAnswerService;

public class ConcludeQuizFunctionalityTCC extends WorkflowFunctionality {
    private CausalQuizAnswer quizAnswer;
    private final QuizAnswerService quizAnswerService;
    private final CausalUnitOfWorkService unitOfWorkService;
    private final CommandGateway commandGateway;

    public ConcludeQuizFunctionalityTCC(QuizAnswerService quizAnswerService, CausalUnitOfWorkService unitOfWorkService,
            Integer quizAggregateId, Integer userAggregateId, CausalUnitOfWork unitOfWork,
            CommandGateway commandGateway) {
        this.quizAnswerService = quizAnswerService;
        this.unitOfWorkService = unitOfWorkService;
        this.commandGateway = commandGateway;
        this.buildWorkflow(quizAggregateId, userAggregateId, unitOfWork);
    }

    public void buildWorkflow(Integer quizAggregateId, Integer userAggregateId, CausalUnitOfWork unitOfWork) {
        this.workflow = new CausalWorkflow(this, unitOfWorkService, unitOfWork);

        SyncStep step = new SyncStep(() -> {
            // quizAnswerService.concludeQuiz(quizAggregateId, userAggregateId, unitOfWork);
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