package pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.quiz.coordination.causal;

import pt.ulisboa.tecnico.socialsoftware.ms.causal.unitOfWork.CausalUnitOfWork;
import pt.ulisboa.tecnico.socialsoftware.ms.causal.unitOfWork.CausalUnitOfWorkService;
import pt.ulisboa.tecnico.socialsoftware.ms.causal.workflow.CausalWorkflow;
import pt.ulisboa.tecnico.socialsoftware.ms.coordination.workflow.Step;
import pt.ulisboa.tecnico.socialsoftware.ms.coordination.workflow.WorkflowFunctionality;
import pt.ulisboa.tecnico.socialsoftware.ms.coordination.workflow.command.CommandGateway;
import pt.ulisboa.tecnico.socialsoftware.quizzes.ServiceMapping;
import pt.ulisboa.tecnico.socialsoftware.quizzes.command.quiz.AddStudentToQuizAnswersCommand;

public class AddStudentToQuizAnswersFunctionalityTCC extends WorkflowFunctionality {
    private final CausalUnitOfWorkService unitOfWorkService;
    private final CommandGateway commandGateway;

    public AddStudentToQuizAnswersFunctionalityTCC(CausalUnitOfWorkService unitOfWorkService,
            Integer quizAggregateId, Integer studentAggregateId,
            CausalUnitOfWork unitOfWork, CommandGateway commandGateway) {
        this.unitOfWorkService = unitOfWorkService;
        this.commandGateway = commandGateway;
        buildWorkflow(quizAggregateId, studentAggregateId, unitOfWork);
    }

    private void buildWorkflow(Integer quizAggregateId, Integer studentAggregateId, CausalUnitOfWork unitOfWork) {
        this.workflow = new CausalWorkflow(this, unitOfWorkService, unitOfWork);
        Step step = new Step("addStudentToQuizAnswers", () -> {
            AddStudentToQuizAnswersCommand command = new AddStudentToQuizAnswersCommand(unitOfWork,
                    ServiceMapping.QUIZ.getServiceName(), quizAggregateId, studentAggregateId);
            commandGateway.send(command);
        });
        workflow.addStep(step);
    }
}
