package pt.ulisboa.tecnico.socialsoftware.ms.quizzes.sagas.coordination.functionalitiesWorkflows;

import java.util.ArrayList;
import java.util.Arrays;

import pt.ulisboa.tecnico.socialsoftware.ms.coordination.workflow.SyncStep;
import pt.ulisboa.tecnico.socialsoftware.ms.coordination.workflow.WorkflowFunctionality;
import pt.ulisboa.tecnico.socialsoftware.ms.quizzes.microservices.topic.service.TopicService;
import pt.ulisboa.tecnico.socialsoftware.ms.quizzes.sagas.aggregates.SagaTopic;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.aggregate.SagaState;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.unityOfWork.SagaUnitOfWork;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.unityOfWork.SagaUnitOfWorkService;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.workflow.SagaWorkflow;

public class DeleteTopicFunctionality extends WorkflowFunctionality {
    private SagaTopic topic;

    private SagaWorkflow workflow;

    private final TopicService topicService;
    private final SagaUnitOfWorkService unitOfWorkService;

    public DeleteTopicFunctionality(TopicService topicService, SagaUnitOfWorkService unitOfWorkService,  
                            Integer topicAggregateId, SagaUnitOfWork unitOfWork) {
        this.topicService = topicService;
        this.unitOfWorkService = unitOfWorkService;
        this.buildWorkflow(topicAggregateId, unitOfWork);
    }

    public void buildWorkflow(Integer topicAggregateId, SagaUnitOfWork unitOfWork) {
        this.workflow = new SagaWorkflow(this, unitOfWorkService, unitOfWork);

        SyncStep getTopicStep = new SyncStep("getTopicStep", () -> {
            SagaTopic topic = (SagaTopic) unitOfWorkService.aggregateLoadAndRegisterRead(topicAggregateId, unitOfWork);
            unitOfWorkService.registerSagaState(topic, SagaState.DELETE_TOPIC_READ_TOPIC, unitOfWork);
            this.setTopic(topic);
        });
    
        getTopicStep.registerCompensation(() -> {
            SagaTopic topic = this.getTopic();
            unitOfWorkService.registerSagaState(topic, SagaState.NOT_IN_SAGA, unitOfWork);
            unitOfWork.registerChanged(topic);
        }, unitOfWork);
    
        SyncStep deleteTopicStep = new SyncStep("deleteTopicStep", () -> {
            topicService.deleteTopic(topicAggregateId, unitOfWork);
        }, new ArrayList<>(Arrays.asList(getTopicStep)));
    
        workflow.addStep(getTopicStep);
        workflow.addStep(deleteTopicStep);
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

    public SagaTopic getTopic() {
        return topic;
    }

    public void setTopic(SagaTopic topic) {
        this.topic = topic;
    }
}