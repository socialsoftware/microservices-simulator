package pt.ulisboa.tecnico.socialsoftware.quizzesfull.microservices.question.coordination.sagas;

import pt.ulisboa.tecnico.socialsoftware.ms.coordination.WorkflowFunctionality;
import pt.ulisboa.tecnico.socialsoftware.ms.messaging.Command;
import pt.ulisboa.tecnico.socialsoftware.ms.messaging.CommandGateway;
import pt.ulisboa.tecnico.socialsoftware.ms.transaction.sagas.aggregate.GenericSagaState;
import pt.ulisboa.tecnico.socialsoftware.ms.transaction.sagas.messaging.SagaCommand;
import pt.ulisboa.tecnico.socialsoftware.ms.transaction.sagas.unitOfWork.SagaUnitOfWork;
import pt.ulisboa.tecnico.socialsoftware.ms.transaction.sagas.unitOfWork.SagaUnitOfWorkService;
import pt.ulisboa.tecnico.socialsoftware.ms.transaction.sagas.workflow.SagaStep;
import pt.ulisboa.tecnico.socialsoftware.ms.transaction.sagas.workflow.SagaWorkflow;
import pt.ulisboa.tecnico.socialsoftware.quizzesfull.ServiceMapping;
import pt.ulisboa.tecnico.socialsoftware.quizzesfull.commands.course.DecrementQuestionCountCommand;
import pt.ulisboa.tecnico.socialsoftware.quizzesfull.commands.question.DeleteQuestionCommand;
import pt.ulisboa.tecnico.socialsoftware.quizzesfull.commands.question.GetQuestionByIdCommand;
import pt.ulisboa.tecnico.socialsoftware.quizzesfull.microservices.question.aggregate.QuestionDto;
import pt.ulisboa.tecnico.socialsoftware.quizzesfull.microservices.question.aggregate.sagas.states.QuestionSagaState;

import java.util.ArrayList;
import java.util.Arrays;

public class DeleteQuestionFunctionalitySagas extends WorkflowFunctionality {
    private QuestionDto questionDto;
    private final SagaUnitOfWorkService unitOfWorkService;
    private final CommandGateway commandGateway;

    public DeleteQuestionFunctionalitySagas(SagaUnitOfWorkService unitOfWorkService,
                                            Integer questionAggregateId,
                                            SagaUnitOfWork unitOfWork, CommandGateway commandGateway) {
        this.unitOfWorkService = unitOfWorkService;
        this.commandGateway = commandGateway;
        this.buildWorkflow(questionAggregateId, unitOfWork);
    }

    public void buildWorkflow(Integer questionAggregateId, SagaUnitOfWork unitOfWork) {
        this.workflow = new SagaWorkflow(this, unitOfWorkService, unitOfWork);

        SagaStep getQuestionStep = new SagaStep("getQuestionStep", () -> {
            GetQuestionByIdCommand getCmd = new GetQuestionByIdCommand(
                    unitOfWork, ServiceMapping.QUESTION.getServiceName(), questionAggregateId);
            SagaCommand sagaCommand = new SagaCommand(getCmd);
            sagaCommand.setSemanticLock(QuestionSagaState.IN_DELETE_QUESTION);
            this.questionDto = (QuestionDto) commandGateway.send(sagaCommand);
        });

        getQuestionStep.registerCompensation(() -> {
            Command command = new Command(unitOfWork, ServiceMapping.QUESTION.getServiceName(), questionAggregateId);
            SagaCommand sagaCommand = new SagaCommand(command);
            sagaCommand.setSemanticLock(GenericSagaState.NOT_IN_SAGA);
            commandGateway.send(sagaCommand);
        }, unitOfWork);

        SagaStep deleteQuestionStep = new SagaStep("deleteQuestionStep", () -> {
            DeleteQuestionCommand deleteCmd = new DeleteQuestionCommand(
                    unitOfWork, ServiceMapping.QUESTION.getServiceName(), questionAggregateId);
            commandGateway.send(deleteCmd);
        }, new ArrayList<>(Arrays.asList(getQuestionStep)));

        SagaStep decrementCourseQuestionCountStep = new SagaStep("decrementCourseQuestionCountStep", () -> {
            DecrementQuestionCountCommand decrementCmd = new DecrementQuestionCountCommand(
                    unitOfWork, ServiceMapping.COURSE.getServiceName(), this.questionDto.getCourseAggregateId());
            commandGateway.send(decrementCmd);
        }, new ArrayList<>(Arrays.asList(deleteQuestionStep)));

        workflow.addStep(getQuestionStep);
        workflow.addStep(deleteQuestionStep);
        workflow.addStep(decrementCourseQuestionCountStep);
    }

    public QuestionDto getQuestionDto() { return questionDto; }
}
