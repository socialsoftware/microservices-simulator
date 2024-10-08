package pt.ulisboa.tecnico.socialsoftware.ms.quizzes.sagas.coordination.workflows;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import pt.ulisboa.tecnico.socialsoftware.ms.coordination.workflow.WorkflowFunctionality;
import pt.ulisboa.tecnico.socialsoftware.ms.quizzes.microservices.question.aggregate.QuestionFactory;
import pt.ulisboa.tecnico.socialsoftware.ms.quizzes.microservices.question.aggregate.QuestionTopic;
import pt.ulisboa.tecnico.socialsoftware.ms.quizzes.microservices.question.service.QuestionService;
import pt.ulisboa.tecnico.socialsoftware.ms.quizzes.microservices.topic.aggregate.TopicDto;
import pt.ulisboa.tecnico.socialsoftware.ms.quizzes.microservices.topic.service.TopicService;
import pt.ulisboa.tecnico.socialsoftware.ms.quizzes.sagas.aggregates.dtos.SagaQuestionDto;
import pt.ulisboa.tecnico.socialsoftware.ms.quizzes.sagas.aggregates.dtos.SagaTopicDto;
import pt.ulisboa.tecnico.socialsoftware.ms.quizzes.sagas.aggregates.states.QuestionSagaState;
import pt.ulisboa.tecnico.socialsoftware.ms.quizzes.sagas.aggregates.states.TopicSagaState;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.aggregate.GenericSagaState;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.unityOfWork.SagaUnitOfWork;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.unityOfWork.SagaUnitOfWorkService;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.workflow.SagaSyncStep;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.workflow.SagaWorkflow;

public class UpdateQuestionTopicsFunctionalitySagas extends WorkflowFunctionality {
    private Set<QuestionTopic> topics;
    private SagaQuestionDto question;
    private Set<TopicDto> topicDtos;
    private final QuestionService questionService;
    private final TopicService topicService;
    private final SagaUnitOfWorkService unitOfWorkService;

    public UpdateQuestionTopicsFunctionalitySagas(QuestionService questionService, TopicService topicService, 
                                QuestionFactory questionFactory, SagaUnitOfWorkService unitOfWorkService,  
                                Integer courseAggregateId, List<Integer> topicIds, SagaUnitOfWork unitOfWork) {
        this.questionService = questionService;
        this.topicService = topicService;
        this.unitOfWorkService = unitOfWorkService;
        this.buildWorkflow(courseAggregateId, topicIds, questionFactory, unitOfWork);
    }

    public void buildWorkflow(Integer courseAggregateId, List<Integer> topicIds, QuestionFactory questionFactory, SagaUnitOfWork unitOfWork) {
        this.workflow = new SagaWorkflow(this, unitOfWorkService, unitOfWork);

        SagaSyncStep getTopicsStep = new SagaSyncStep("getTopicsStep", () -> {
            Set<QuestionTopic> topics = topicIds.stream()
                .map(topicId -> {
                    SagaTopicDto topic = (SagaTopicDto) topicService.getTopicById(topicId, unitOfWork);
                    unitOfWorkService.registerSagaState(topic.getAggregateId(), TopicSagaState.READ_TOPIC, unitOfWork);
                    return topic;
                })
                .map(QuestionTopic::new)
                .collect(Collectors.toSet());
            this.setTopics(topics);
        });

        getTopicsStep.registerCompensation(() -> {
            topicIds.forEach(topicId -> {
                unitOfWorkService.registerSagaState(topicId, GenericSagaState.NOT_IN_SAGA, unitOfWork);
            });
        }, unitOfWork);
    
        SagaSyncStep getQuestionStep = new SagaSyncStep("getQuestionStep", () -> {
            SagaQuestionDto question = (SagaQuestionDto) questionService.getQuestionById(courseAggregateId, unitOfWork);
            unitOfWorkService.registerSagaState(question.getAggregateId(), QuestionSagaState.READ_QUESTION, unitOfWork);
            Set<TopicDto> topics = question.getTopicDto();
            this.setQuestion(question);
            this.setTopicDtos(topics);
        });
    
        getQuestionStep.registerCompensation(() -> {
            unitOfWorkService.registerSagaState(question.getAggregateId(), GenericSagaState.NOT_IN_SAGA, unitOfWork);
        }, unitOfWork);
    
        SagaSyncStep updateQuestionTopicsStep = new SagaSyncStep("updateQuestionTopicsStep", () -> {
            questionService.updateQuestionTopics(courseAggregateId, this.getTopics(), unitOfWork);
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

    public SagaQuestionDto getQuestion() {
        return question;
    }

    public void setQuestion(SagaQuestionDto question) {
        this.question = question;
    }
}