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
import pt.ulisboa.tecnico.socialsoftware.quizzesfull.commands.question.GetQuestionByIdCommand;
import pt.ulisboa.tecnico.socialsoftware.quizzesfull.commands.question.UpdateQuestionCommand;
import pt.ulisboa.tecnico.socialsoftware.quizzesfull.commands.topic.GetTopicByIdCommand;
import pt.ulisboa.tecnico.socialsoftware.quizzesfull.microservices.question.aggregate.QuestionDto;
import pt.ulisboa.tecnico.socialsoftware.quizzesfull.microservices.question.aggregate.QuestionTopic;
import pt.ulisboa.tecnico.socialsoftware.quizzesfull.microservices.question.aggregate.sagas.states.QuestionSagaState;
import pt.ulisboa.tecnico.socialsoftware.quizzesfull.microservices.topic.aggregate.TopicDto;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class UpdateQuestionFunctionalitySagas extends WorkflowFunctionality {
    private QuestionDto questionDto;
    private Set<QuestionTopic> questionTopics;
    private final SagaUnitOfWorkService unitOfWorkService;
    private final CommandGateway commandGateway;

    public UpdateQuestionFunctionalitySagas(SagaUnitOfWorkService unitOfWorkService,
                                            Integer questionAggregateId, String title, String content,
                                            List<Integer> topicIds,
                                            SagaUnitOfWork unitOfWork, CommandGateway commandGateway) {
        this.unitOfWorkService = unitOfWorkService;
        this.commandGateway = commandGateway;
        this.buildWorkflow(questionAggregateId, title, content, topicIds, unitOfWork);
    }

    public void buildWorkflow(Integer questionAggregateId, String title, String content,
                              List<Integer> topicIds, SagaUnitOfWork unitOfWork) {
        this.workflow = new SagaWorkflow(this, unitOfWorkService, unitOfWork);

        SagaStep getQuestionStep = new SagaStep("getQuestionStep", () -> {
            GetQuestionByIdCommand getCmd = new GetQuestionByIdCommand(
                    unitOfWork, ServiceMapping.QUESTION.getServiceName(), questionAggregateId);
            SagaCommand sagaCommand = new SagaCommand(getCmd);
            sagaCommand.setSemanticLock(QuestionSagaState.IN_UPDATE_QUESTION);
            this.questionDto = (QuestionDto) commandGateway.send(sagaCommand);
        });

        getQuestionStep.registerCompensation(() -> {
            Command command = new Command(unitOfWork, ServiceMapping.QUESTION.getServiceName(), questionAggregateId);
            SagaCommand sagaCommand = new SagaCommand(command);
            sagaCommand.setSemanticLock(GenericSagaState.NOT_IN_SAGA);
            commandGateway.send(sagaCommand);
        }, unitOfWork);

        SagaStep getTopicsStep = new SagaStep("getTopicsStep", () -> {
            this.questionTopics = new HashSet<>();
            for (Integer topicId : topicIds) {
                GetTopicByIdCommand getTopicCmd = new GetTopicByIdCommand(
                        unitOfWork, ServiceMapping.TOPIC.getServiceName(), topicId);
                TopicDto topicDto = (TopicDto) commandGateway.send(getTopicCmd);
                this.questionTopics.add(new QuestionTopic(topicDto));
            }
        }, new ArrayList<>(Arrays.asList(getQuestionStep)));

        SagaStep updateQuestionStep = new SagaStep("updateQuestionStep", () -> {
            UpdateQuestionCommand updateCmd = new UpdateQuestionCommand(
                    unitOfWork, ServiceMapping.QUESTION.getServiceName(),
                    questionAggregateId, title, content, this.questionTopics);
            commandGateway.send(updateCmd);
        }, new ArrayList<>(Arrays.asList(getTopicsStep)));

        workflow.addStep(getQuestionStep);
        workflow.addStep(getTopicsStep);
        workflow.addStep(updateQuestionStep);
    }

    public QuestionDto getQuestionDto() { return questionDto; }
}
