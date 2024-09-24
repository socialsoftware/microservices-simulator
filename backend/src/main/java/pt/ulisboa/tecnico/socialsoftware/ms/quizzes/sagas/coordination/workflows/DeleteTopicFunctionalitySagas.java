package pt.ulisboa.tecnico.socialsoftware.ms.quizzes.sagas.coordination.workflows;

import java.util.ArrayList;
import java.util.Arrays;

import pt.ulisboa.tecnico.socialsoftware.ms.coordination.workflow.SyncStep;
import pt.ulisboa.tecnico.socialsoftware.ms.coordination.workflow.WorkflowFunctionality;
import pt.ulisboa.tecnico.socialsoftware.ms.quizzes.microservices.topic.service.TopicService;
import pt.ulisboa.tecnico.socialsoftware.ms.quizzes.sagas.aggregates.SagaTopic;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.aggregate.GenericSagaState;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.aggregate.SagaState;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.unityOfWork.SagaUnitOfWork;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.unityOfWork.SagaUnitOfWorkService;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.workflow.SagaWorkflow;

public class DeleteTopicFunctionalitySagas extends WorkflowFunctionality {
    public enum State implements SagaState {
        DELETE_TOPIC_READ_TOPIC {
            @Override
            public String getStateName() {
                return "DELETE_TOPIC_READ_TOPIC";
            }
        }
    }
    private SagaTopic topic;

    

    private final TopicService topicService;
    private final SagaUnitOfWorkService unitOfWorkService;

    public DeleteTopicFunctionalitySagas(TopicService topicService, SagaUnitOfWorkService unitOfWorkService,  
                            Integer topicAggregateId, SagaUnitOfWork unitOfWork) {
        this.topicService = topicService;
        this.unitOfWorkService = unitOfWorkService;
        this.buildWorkflow(topicAggregateId, unitOfWork);
    }

    public void buildWorkflow(Integer topicAggregateId, SagaUnitOfWork unitOfWork) {
        this.workflow = new SagaWorkflow(this, unitOfWorkService, unitOfWork);

        SyncStep getTopicStep = new SyncStep("getTopicStep", () -> {
            SagaTopic topic = (SagaTopic) unitOfWorkService.aggregateLoadAndRegisterRead(topicAggregateId, unitOfWork);
            unitOfWorkService.registerSagaState(topic, State.DELETE_TOPIC_READ_TOPIC, unitOfWork);
            this.setTopic(topic);
        });
    
        getTopicStep.registerCompensation(() -> {
            SagaTopic topic = this.getTopic();
            unitOfWorkService.registerSagaState(topic, GenericSagaState.NOT_IN_SAGA, unitOfWork);
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

    public SagaTopic getTopic() {
        return topic;
    }

    public void setTopic(SagaTopic topic) {
        this.topic = topic;
    }
}