package pt.ulisboa.tecnico.socialsoftware.answers.microservices.answer.coordination.sagas;

import pt.ulisboa.tecnico.socialsoftware.ms.coordination.workflow.WorkflowFunctionality;
import pt.ulisboa.tecnico.socialsoftware.ms.coordination.workflow.CommandGateway;
import pt.ulisboa.tecnico.socialsoftware.answers.ServiceMapping;
import pt.ulisboa.tecnico.socialsoftware.answers.command.answer.*;
import pt.ulisboa.tecnico.socialsoftware.answers.shared.dtos.AnswerQuestionDto;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.unitOfWork.SagaUnitOfWork;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.unitOfWork.SagaUnitOfWorkService;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.workflow.SagaStep;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.workflow.SagaWorkflow;

public class AddAnswerQuestionFunctionalitySagas extends WorkflowFunctionality {
    private AnswerQuestionDto addedQuestionDto;
    private final SagaUnitOfWorkService unitOfWorkService;
    private final CommandGateway commandGateway;

    public AddAnswerQuestionFunctionalitySagas(SagaUnitOfWorkService unitOfWorkService, Integer answerId, Integer questionAggregateId, AnswerQuestionDto questionDto, SagaUnitOfWork unitOfWork, CommandGateway commandGateway) {
        this.unitOfWorkService = unitOfWorkService;
        this.commandGateway = commandGateway;
        this.buildWorkflow(answerId, questionAggregateId, questionDto, unitOfWork);
    }

    public void buildWorkflow(Integer answerId, Integer questionAggregateId, AnswerQuestionDto questionDto, SagaUnitOfWork unitOfWork) {
        this.workflow = new SagaWorkflow(this, unitOfWorkService, unitOfWork);

        SagaStep addQuestionStep = new SagaStep("addQuestionStep", () -> {
            AddAnswerQuestionCommand cmd = new AddAnswerQuestionCommand(unitOfWork, ServiceMapping.ANSWER.getServiceName(), answerId, questionAggregateId, questionDto);
            AnswerQuestionDto addedQuestionDto = (AnswerQuestionDto) commandGateway.send(cmd);
            setAddedQuestionDto(addedQuestionDto);
        });

        workflow.addStep(addQuestionStep);
    }
    public AnswerQuestionDto getAddedQuestionDto() {
        return addedQuestionDto;
    }

    public void setAddedQuestionDto(AnswerQuestionDto addedQuestionDto) {
        this.addedQuestionDto = addedQuestionDto;
    }
}
