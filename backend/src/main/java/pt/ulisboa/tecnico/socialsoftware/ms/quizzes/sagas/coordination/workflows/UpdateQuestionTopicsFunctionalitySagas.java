package pt.ulisboa.tecnico.socialsoftware.ms.quizzes.sagas.coordination.workflows;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import pt.ulisboa.tecnico.socialsoftware.ms.coordination.workflow.WorkflowFunctionality;
import pt.ulisboa.tecnico.socialsoftware.ms.quizzes.microservices.question.aggregate.Question;
import pt.ulisboa.tecnico.socialsoftware.ms.quizzes.microservices.question.aggregate.QuestionFactory;
import pt.ulisboa.tecnico.socialsoftware.ms.quizzes.microservices.question.aggregate.QuestionTopic;
import pt.ulisboa.tecnico.socialsoftware.ms.quizzes.microservices.question.service.QuestionService;
import pt.ulisboa.tecnico.socialsoftware.ms.quizzes.microservices.topic.service.TopicService;
import pt.ulisboa.tecnico.socialsoftware.ms.quizzes.sagas.aggregates.SagaQuestion;
import pt.ulisboa.tecnico.socialsoftware.ms.quizzes.sagas.aggregates.states.QuestionSagaState;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.aggregate.GenericSagaState;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.unityOfWork.SagaUnitOfWork;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.unityOfWork.SagaUnitOfWorkService;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.workflow.SagaSyncStep;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.workflow.SagaWorkflow;

public class UpdateQuestionTopicsFunctionalitySagas extends WorkflowFunctionality {
    private Set<QuestionTopic> topics;
    private Question oldQuestion;
    private Set<QuestionTopic> oldTopics;

    

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
                            .map(id -> topicService.getTopicById(id, unitOfWork))
                            .map(QuestionTopic::new)
                            .collect(Collectors.toSet());
            this.setTopics(topics);
        });
    
        SagaSyncStep getOldQuestionStep = new SagaSyncStep("getOldQuestionStep", () -> {
            SagaQuestion oldQuestion = (SagaQuestion) unitOfWorkService.aggregateLoadAndRegisterRead(courseAggregateId, unitOfWork);
            unitOfWorkService.registerSagaState(oldQuestion, QuestionSagaState.READ_QUESTION, unitOfWork);
            Set<QuestionTopic> oldTopics = oldQuestion.getQuestionTopics();
            this.setOldQuestion(oldQuestion);
            this.setOldTopics(oldTopics);
        });
    
        getOldQuestionStep.registerCompensation(() -> {
            Question newQuestion = questionFactory.createQuestionFromExisting(this.getOldQuestion());
            newQuestion.setQuestionTopics(this.getOldTopics());
            unitOfWorkService.registerSagaState((SagaQuestion) newQuestion, GenericSagaState.NOT_IN_SAGA, unitOfWork);
            unitOfWork.registerChanged(newQuestion);
        }, unitOfWork);
    
        SagaSyncStep updateQuestionTopicsStep = new SagaSyncStep("updateQuestionTopicsStep", () -> {
            questionService.updateQuestionTopics(courseAggregateId, this.getTopics(), unitOfWork);
        }, new ArrayList<>(Arrays.asList(getTopicsStep, getOldQuestionStep)));
    
        workflow.addStep(getTopicsStep);
        workflow.addStep(getOldQuestionStep);
        workflow.addStep(updateQuestionTopicsStep);
    }

    @Override
    public void handleEvents() {

    }

    public Set<QuestionTopic> getTopics() {
        return topics;
    }

    public void setTopics(Set<QuestionTopic> topics) {
        this.topics = topics;
    }

    public Question getOldQuestion() {
        return oldQuestion;
    }

    public void setOldQuestion(Question oldQuestion) {
        this.oldQuestion = oldQuestion;
    }

    public Set<QuestionTopic> getOldTopics() {
        return oldTopics;
    }

    public void setOldTopics(Set<QuestionTopic> oldTopics) {
        this.oldTopics = oldTopics;
    }
}