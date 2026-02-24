package pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.question.coordination.causal;

import pt.ulisboa.tecnico.socialsoftware.ms.causal.unitOfWork.CausalUnitOfWork;
import pt.ulisboa.tecnico.socialsoftware.ms.causal.unitOfWork.CausalUnitOfWorkService;
import pt.ulisboa.tecnico.socialsoftware.ms.causal.workflow.CausalWorkflow;
import pt.ulisboa.tecnico.socialsoftware.ms.coordination.workflow.CommandGateway;
import pt.ulisboa.tecnico.socialsoftware.ms.coordination.workflow.Step;
import pt.ulisboa.tecnico.socialsoftware.ms.coordination.workflow.WorkflowFunctionality;
import pt.ulisboa.tecnico.socialsoftware.quizzes.ServiceMapping;
import pt.ulisboa.tecnico.socialsoftware.quizzes.command.question.GetQuestionByIdCommand;
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.question.aggregate.QuestionDto;

public class FindQuestionByAggregateIdFunctionalityTCC extends WorkflowFunctionality {
    private QuestionDto questionDto;
    private final CausalUnitOfWorkService unitOfWorkService;
    private final CommandGateway commandGateway;

    public FindQuestionByAggregateIdFunctionalityTCC(CausalUnitOfWorkService unitOfWorkService,
                                                     Integer aggregateId, CausalUnitOfWork unitOfWork, CommandGateway commandGateway) {
        this.unitOfWorkService = unitOfWorkService;
        this.commandGateway = commandGateway;
        this.buildWorkflow(aggregateId, unitOfWork);
    }

    public void buildWorkflow(Integer aggregateId, CausalUnitOfWork unitOfWork) {
        this.workflow = new CausalWorkflow(this, unitOfWorkService, unitOfWork);

        Step step = new Step(() -> {
            GetQuestionByIdCommand getQuestionByIdCommand = new GetQuestionByIdCommand(unitOfWork,
                    ServiceMapping.QUESTION.getServiceName(), aggregateId);
            this.questionDto = (QuestionDto) commandGateway.send(getQuestionByIdCommand);
        });

        workflow.addStep(step);
    }

    public QuestionDto getQuestionDto() {
        return questionDto;
    }

    public void setQuestionDto(QuestionDto questionDto) {
        this.questionDto = questionDto;
    }
}