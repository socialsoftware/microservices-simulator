package pt.ulisboa.tecnico.socialsoftware.ms.quizzes.sagas.coordination.functionalitiesWorkflows;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import pt.ulisboa.tecnico.socialsoftware.ms.coordination.workflow.SyncStep;
import pt.ulisboa.tecnico.socialsoftware.ms.coordination.workflow.WorkflowFunctionality;
import pt.ulisboa.tecnico.socialsoftware.ms.quizzes.microservices.question.aggregate.Question;
import pt.ulisboa.tecnico.socialsoftware.ms.quizzes.microservices.question.aggregate.QuestionFactory;
import pt.ulisboa.tecnico.socialsoftware.ms.quizzes.microservices.question.aggregate.QuestionTopic;
import pt.ulisboa.tecnico.socialsoftware.ms.quizzes.microservices.question.service.QuestionService;
import pt.ulisboa.tecnico.socialsoftware.ms.quizzes.microservices.topic.service.TopicService;
import pt.ulisboa.tecnico.socialsoftware.ms.quizzes.sagas.aggregates.SagaQuestion;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.aggregate.SagaState;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.unityOfWork.SagaUnitOfWork;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.unityOfWork.SagaUnitOfWorkService;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.workflow.SagaWorkflow;

public class UpdateQuestionTopicsFunctionality extends WorkflowFunctionality {
    private Set<QuestionTopic> topics;
    private Question oldQuestion;
    private Set<QuestionTopic> oldTopics;

    private SagaWorkflow workflow;

    private final QuestionService questionService;
    private final TopicService topicService;
    private final SagaUnitOfWorkService unitOfWorkService;

    public UpdateQuestionTopicsFunctionality(QuestionService questionService, TopicService topicService, 
                                QuestionFactory questionFactory, SagaUnitOfWorkService unitOfWorkService,  
                                Integer courseAggregateId, List<Integer> topicIds, SagaUnitOfWork unitOfWork) {
        this.questionService = questionService;
        this.topicService = topicService;
        this.unitOfWorkService = unitOfWorkService;
        this.buildWorkflow(courseAggregateId, topicIds, questionFactory, unitOfWork);
    }

    public void buildWorkflow(Integer courseAggregateId, List<Integer> topicIds, QuestionFactory questionFactory, SagaUnitOfWork unitOfWork) {
        this.workflow = new SagaWorkflow(this, unitOfWorkService, unitOfWork);

        SyncStep getTopicsStep = new SyncStep(() -> {
            Set<QuestionTopic> topics = topicIds.stream()
                            .map(id -> topicService.getTopicById(id, unitOfWork))
                            .map(QuestionTopic::new)
                            .collect(Collectors.toSet());
            this.setTopics(topics);
        });
    
        SyncStep getOldQuestionStep = new SyncStep(() -> {
            SagaQuestion oldQuestion = (SagaQuestion) unitOfWorkService.aggregateLoadAndRegisterRead(courseAggregateId, unitOfWork);
            unitOfWorkService.registerSagaState(oldQuestion, SagaState.UPDATE_QUESTION_TOPICS_READ_QUESTION, unitOfWork);
            Set<QuestionTopic> oldTopics = oldQuestion.getQuestionTopics();
            this.setOldQuestion(oldQuestion);
            this.setOldTopics(oldTopics);
        });
    
        getOldQuestionStep.registerCompensation(() -> {
            Question newQuestion = questionFactory.createQuestionFromExisting(this.getOldQuestion());
            newQuestion.setQuestionTopics(this.getOldTopics());
            unitOfWorkService.registerSagaState((SagaQuestion) newQuestion, SagaState.NOT_IN_SAGA, unitOfWork);
            unitOfWork.registerChanged(newQuestion);
        }, unitOfWork);
    
        SyncStep updateQuestionTopicsStep = new SyncStep(() -> {
            questionService.updateQuestionTopics(courseAggregateId, this.getTopics(), unitOfWork);
        }, new ArrayList<>(Arrays.asList(getTopicsStep, getOldQuestionStep)));
    
        workflow.addStep(getTopicsStep);
        workflow.addStep(getOldQuestionStep);
        workflow.addStep(updateQuestionTopicsStep);
    }

    @Override
    public void handleEvents() {

    }

    public void executeWorkflow(SagaUnitOfWork unitOfWork) {
        workflow.execute(unitOfWork);
    }

    public void executeStepByName(String stepName, SagaUnitOfWork unitOfWork) {
        workflow.executeStepByName(stepName, unitOfWork);
    }

    public void executeUntilStep(String stepName, SagaUnitOfWork unitOfWork) {
        workflow.executeUntilStep(stepName, unitOfWork);
    }

    public void resumeWorkflow(SagaUnitOfWork unitOfWork) {
        workflow.resume(unitOfWork);
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