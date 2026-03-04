package pt.ulisboa.tecnico.socialsoftware.answers.microservices.answer.coordination.sagas;

import pt.ulisboa.tecnico.socialsoftware.ms.coordination.workflow.WorkflowFunctionality;
import pt.ulisboa.tecnico.socialsoftware.ms.coordination.workflow.CommandGateway;
import pt.ulisboa.tecnico.socialsoftware.answers.ServiceMapping;
import pt.ulisboa.tecnico.socialsoftware.answers.command.answer.*;
import pt.ulisboa.tecnico.socialsoftware.answers.shared.dtos.AnswerDto;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.unitOfWork.SagaUnitOfWork;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.unitOfWork.SagaUnitOfWorkService;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.workflow.SagaStep;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.workflow.SagaWorkflow;

public class GetAnswerByIdFunctionalitySagas extends WorkflowFunctionality {
    private AnswerDto answerDto;
    private final SagaUnitOfWorkService unitOfWorkService;
    private final CommandGateway commandGateway;


    public GetAnswerByIdFunctionalitySagas(SagaUnitOfWorkService unitOfWorkService, Integer answerAggregateId, SagaUnitOfWork unitOfWork, CommandGateway commandGateway) {
        this.unitOfWorkService = unitOfWorkService;
        this.commandGateway = commandGateway;
        this.buildWorkflow(answerAggregateId, unitOfWork);
    }

    public void buildWorkflow(Integer answerAggregateId, SagaUnitOfWork unitOfWork) {
        this.workflow = new SagaWorkflow(this, unitOfWorkService, unitOfWork);

        SagaStep getAnswerStep = new SagaStep("getAnswerStep", () -> {
            GetAnswerByIdCommand cmd = new GetAnswerByIdCommand(unitOfWork, ServiceMapping.ANSWER.getServiceName(), answerAggregateId);
            AnswerDto answerDto = (AnswerDto) commandGateway.send(cmd);
            setAnswerDto(answerDto);
        });

        workflow.addStep(getAnswerStep);
    }
    public AnswerDto getAnswerDto() {
        return answerDto;
    }

    public void setAnswerDto(AnswerDto answerDto) {
        this.answerDto = answerDto;
    }
}
