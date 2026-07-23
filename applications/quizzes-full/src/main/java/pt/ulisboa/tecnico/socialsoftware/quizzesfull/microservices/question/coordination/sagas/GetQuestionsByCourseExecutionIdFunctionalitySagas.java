package pt.ulisboa.tecnico.socialsoftware.quizzesfull.microservices.question.coordination.sagas;

import pt.ulisboa.tecnico.socialsoftware.ms.coordination.WorkflowFunctionality;
import pt.ulisboa.tecnico.socialsoftware.ms.messaging.CommandGateway;
import pt.ulisboa.tecnico.socialsoftware.ms.transaction.sagas.unitOfWork.SagaUnitOfWork;
import pt.ulisboa.tecnico.socialsoftware.ms.transaction.sagas.unitOfWork.SagaUnitOfWorkService;
import pt.ulisboa.tecnico.socialsoftware.ms.transaction.sagas.workflow.SagaStep;
import pt.ulisboa.tecnico.socialsoftware.ms.transaction.sagas.workflow.SagaWorkflow;
import pt.ulisboa.tecnico.socialsoftware.quizzesfull.ServiceMapping;
import pt.ulisboa.tecnico.socialsoftware.quizzesfull.commands.execution.GetExecutionByIdCommand;
import pt.ulisboa.tecnico.socialsoftware.quizzesfull.commands.question.GetQuestionsByCourseExecutionIdCommand;
import pt.ulisboa.tecnico.socialsoftware.quizzesfull.microservices.execution.aggregate.ExecutionDto;
import pt.ulisboa.tecnico.socialsoftware.quizzesfull.microservices.question.aggregate.QuestionDto;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class GetQuestionsByCourseExecutionIdFunctionalitySagas extends WorkflowFunctionality {
    private List<QuestionDto> questions;
    private ExecutionDto executionDto;
    private final SagaUnitOfWorkService unitOfWorkService;
    private final CommandGateway commandGateway;

    public GetQuestionsByCourseExecutionIdFunctionalitySagas(SagaUnitOfWorkService unitOfWorkService,
                                                              Integer executionAggregateId,
                                                              SagaUnitOfWork unitOfWork, CommandGateway commandGateway) {
        this.unitOfWorkService = unitOfWorkService;
        this.commandGateway = commandGateway;
        buildWorkflow(executionAggregateId, unitOfWork);
    }

    public void buildWorkflow(Integer executionAggregateId, SagaUnitOfWork unitOfWork) {
        this.workflow = new SagaWorkflow(this, unitOfWorkService, unitOfWork);

        SagaStep getExecutionStep = new SagaStep("getExecutionStep", () -> {
            GetExecutionByIdCommand cmd = new GetExecutionByIdCommand(
                    unitOfWork, ServiceMapping.EXECUTION.getServiceName(), executionAggregateId);
            this.executionDto = (ExecutionDto) commandGateway.send(cmd);
        });

        SagaStep getQuestionsStep = new SagaStep("getQuestionsStep", () -> {
            GetQuestionsByCourseExecutionIdCommand cmd = new GetQuestionsByCourseExecutionIdCommand(
                    unitOfWork, ServiceMapping.QUESTION.getServiceName(), executionDto.getCourseId());
            this.questions = (List<QuestionDto>) commandGateway.send(cmd);
        }, new ArrayList<>(Arrays.asList(getExecutionStep)));

        this.workflow.addStep(getExecutionStep);
        this.workflow.addStep(getQuestionsStep);
    }

    public List<QuestionDto> getQuestions() {
        return questions;
    }
}
