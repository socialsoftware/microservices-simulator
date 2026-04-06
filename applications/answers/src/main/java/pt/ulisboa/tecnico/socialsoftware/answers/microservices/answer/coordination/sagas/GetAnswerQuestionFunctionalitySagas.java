package pt.ulisboa.tecnico.socialsoftware.answers.microservices.answer.coordination.sagas;

import pt.ulisboa.tecnico.socialsoftware.ms.coordination.workflow.WorkflowFunctionality;
import pt.ulisboa.tecnico.socialsoftware.ms.coordination.workflow.command.CommandGateway;
import pt.ulisboa.tecnico.socialsoftware.answers.ServiceMapping;
import pt.ulisboa.tecnico.socialsoftware.answers.command.answer.*;
import pt.ulisboa.tecnico.socialsoftware.answers.shared.dtos.AnswerQuestionDto;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.unitOfWork.SagaUnitOfWork;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.unitOfWork.SagaUnitOfWorkService;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.workflow.SagaStep;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.workflow.SagaWorkflow;

public class GetAnswerQuestionFunctionalitySagas extends WorkflowFunctionality {
    private AnswerQuestionDto questionDto;
    private final SagaUnitOfWorkService unitOfWorkService;
    private final CommandGateway commandGateway;

    public GetAnswerQuestionFunctionalitySagas(SagaUnitOfWorkService unitOfWorkService, Integer answerId, Integer questionAggregateId, SagaUnitOfWork unitOfWork, CommandGateway commandGateway) {
        this.unitOfWorkService = unitOfWorkService;
        this.commandGateway = commandGateway;
        this.buildWorkflow(answerId, questionAggregateId, unitOfWork);
    }

    public void buildWorkflow(Integer answerId, Integer questionAggregateId, SagaUnitOfWork unitOfWork) {
        this.workflow = new SagaWorkflow(this, unitOfWorkService, unitOfWork);

        SagaStep getQuestionStep = new SagaStep("getQuestionStep", () -> {
            GetAnswerQuestionCommand cmd = new GetAnswerQuestionCommand(unitOfWork, ServiceMapping.ANSWER.getServiceName(), answerId, questionAggregateId);
            AnswerQuestionDto questionDto = (AnswerQuestionDto) commandGateway.send(cmd);
            setQuestionDto(questionDto);
        });

        workflow.addStep(getQuestionStep);
    }
    public AnswerQuestionDto getQuestionDto() {
        return questionDto;
    }

    public void setQuestionDto(AnswerQuestionDto questionDto) {
        this.questionDto = questionDto;
    }
}
