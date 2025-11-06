package pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.question.coordination.sagas;

import pt.ulisboa.tecnico.socialsoftware.ms.coordination.workflow.Command;
import pt.ulisboa.tecnico.socialsoftware.ms.coordination.workflow.CommandGateway;
import pt.ulisboa.tecnico.socialsoftware.ms.coordination.workflow.WorkflowFunctionality;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.aggregate.GenericSagaState;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.unitOfWork.SagaUnitOfWork;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.unitOfWork.SagaUnitOfWorkService;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.workflow.SagaSyncStep;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.workflow.SagaWorkflow;
import pt.ulisboa.tecnico.socialsoftware.quizzes.ServiceMapping;
import pt.ulisboa.tecnico.socialsoftware.quizzes.command.question.GetQuestionByIdCommand;
import pt.ulisboa.tecnico.socialsoftware.quizzes.command.question.UpdateQuestionTopicsCommand;
import pt.ulisboa.tecnico.socialsoftware.quizzes.command.topic.GetTopicByIdCommand;
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.question.aggregate.QuestionDto;
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.question.aggregate.QuestionTopic;
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.question.aggregate.sagas.states.QuestionSagaState;
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.topic.aggregate.TopicDto;
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.topic.aggregate.sagas.states.TopicSagaState;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public class UpdateQuestionTopicsFunctionalitySagas extends WorkflowFunctionality {
    private Set<QuestionTopic> topics;
    private QuestionDto question;
    private Set<TopicDto> topicDtos;
    private final SagaUnitOfWorkService unitOfWorkService;
    private final CommandGateway commandGateway;

    public UpdateQuestionTopicsFunctionalitySagas(SagaUnitOfWorkService unitOfWorkService,
                                                  Integer courseAggregateId, List<Integer> topicIds, SagaUnitOfWork unitOfWork,
                                                  CommandGateway CommandGateway) {
        this.unitOfWorkService = unitOfWorkService;
        this.commandGateway = CommandGateway;
        this.buildWorkflow(courseAggregateId, topicIds, unitOfWork);
    }

    public void buildWorkflow(Integer courseAggregateId, List<Integer> topicIds, SagaUnitOfWork unitOfWork) {
        this.workflow = new SagaWorkflow(this, unitOfWorkService, unitOfWork);

        SagaSyncStep getTopicsStep = new SagaSyncStep("getTopicsStep", () -> { // TODO
            Set<QuestionTopic> topics = topicIds.stream()
                    .map(topicId -> {
                        GetTopicByIdCommand getTopicByIdCommand = new GetTopicByIdCommand(unitOfWork, ServiceMapping.TOPIC.getServiceName(), topicId);
                        getTopicByIdCommand.setSemanticLock(TopicSagaState.READ_TOPIC);
                        return (TopicDto) commandGateway.send(getTopicByIdCommand);
                    })
                    .map(QuestionTopic::new)
                    .collect(Collectors.toSet());
            this.setTopics(topics);
        });

        getTopicsStep.registerCompensation(() -> {
            topicIds.forEach(topicId -> {
                Command command = new Command(unitOfWork, ServiceMapping.TOPIC.getServiceName(), topicId);
                command.setSemanticLock(GenericSagaState.NOT_IN_SAGA);
                commandGateway.send(command);
            });
        }, unitOfWork);

        SagaSyncStep getQuestionStep = new SagaSyncStep("getQuestionStep", () -> {
            GetQuestionByIdCommand getQuestionByIdCommand = new GetQuestionByIdCommand(unitOfWork, ServiceMapping.QUESTION.getServiceName(), courseAggregateId);
            getQuestionByIdCommand.setSemanticLock(QuestionSagaState.READ_QUESTION);
            QuestionDto question = (QuestionDto) commandGateway.send(getQuestionByIdCommand);
            Set<TopicDto> topics = question.getTopicDto();
            this.setQuestion(question);
            this.setTopicDtos(topics);
        });

        getQuestionStep.registerCompensation(() -> {
            Command command = new Command(unitOfWork, ServiceMapping.QUESTION.getServiceName(), question.getAggregateId());
            command.setSemanticLock(GenericSagaState.NOT_IN_SAGA);
            commandGateway.send(command);
        }, unitOfWork);

        SagaSyncStep updateQuestionTopicsStep = new SagaSyncStep("updateQuestionTopicsStep", () -> {
            UpdateQuestionTopicsCommand updateQuestionTopicsCommand = new UpdateQuestionTopicsCommand(unitOfWork, ServiceMapping.QUESTION.getServiceName(), courseAggregateId, this.getTopics());
            commandGateway.send(updateQuestionTopicsCommand);
        }, new ArrayList<>(Arrays.asList(getTopicsStep, getQuestionStep)));

        workflow.addStep(getTopicsStep);
        workflow.addStep(getQuestionStep);
        workflow.addStep(updateQuestionTopicsStep);
    }

    public Set<QuestionTopic> getTopics() {
        return topics;
    }

    public void setTopics(Set<QuestionTopic> topics) {
        this.topics = topics;
    }

    public Set<TopicDto> getTopicDtos() {
        return topicDtos;
    }

    public void setTopicDtos(Set<TopicDto> topics) {
        this.topicDtos = topics;
    }

    public QuestionDto getQuestion() {
        return question;
    }

    public void setQuestion(QuestionDto question) {
        this.question = question;
    }
}