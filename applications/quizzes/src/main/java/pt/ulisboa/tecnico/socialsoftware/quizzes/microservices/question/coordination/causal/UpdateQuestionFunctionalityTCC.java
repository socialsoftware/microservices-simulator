package pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.question.coordination.causal;

import pt.ulisboa.tecnico.socialsoftware.ms.causal.unitOfWork.CausalUnitOfWork;
import pt.ulisboa.tecnico.socialsoftware.ms.causal.unitOfWork.CausalUnitOfWorkService;
import pt.ulisboa.tecnico.socialsoftware.ms.causal.workflow.CausalWorkflow;
import pt.ulisboa.tecnico.socialsoftware.ms.coordination.workflow.CommandGateway;
import pt.ulisboa.tecnico.socialsoftware.ms.coordination.workflow.Step;
import pt.ulisboa.tecnico.socialsoftware.ms.coordination.workflow.WorkflowFunctionality;
import pt.ulisboa.tecnico.socialsoftware.quizzes.ServiceMapping;
import pt.ulisboa.tecnico.socialsoftware.quizzes.command.question.UpdateQuestionCommand;
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.question.aggregate.Question;
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.question.aggregate.QuestionDto;

public class UpdateQuestionFunctionalityTCC extends WorkflowFunctionality {
    private Question oldQuestion;
    private final CausalUnitOfWorkService unitOfWorkService;
    private final CommandGateway commandGateway;

    public UpdateQuestionFunctionalityTCC(CausalUnitOfWorkService unitOfWorkService,
                                          QuestionDto questionDto, CausalUnitOfWork unitOfWork,
                                          CommandGateway commandGateway) {
        this.unitOfWorkService = unitOfWorkService;
        this.commandGateway = commandGateway;
        this.buildWorkflow(questionDto, unitOfWork);
    }

    public void buildWorkflow(QuestionDto questionDto, CausalUnitOfWork unitOfWork) {
        this.workflow = new CausalWorkflow(this, unitOfWorkService, unitOfWork);

        Step step = new Step(() -> {
            UpdateQuestionCommand cmd = new UpdateQuestionCommand(unitOfWork, ServiceMapping.QUESTION.getServiceName(), questionDto);
            commandGateway.send(cmd);
        });

        workflow.addStep(step);
    }

    public Question getOldQuestion() {
        return oldQuestion;
    }

    public void setOldQuestion(Question oldQuestion) {
        this.oldQuestion = oldQuestion;
    }
}