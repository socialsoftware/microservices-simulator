package pt.ulisboa.tecnico.socialsoftware.quizzes.sagas.coordination.topic;

import pt.ulisboa.tecnico.socialsoftware.ms.coordination.workflow.WorkflowFunctionality;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.aggregate.GenericSagaState;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.unitOfWork.SagaUnitOfWork;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.unitOfWork.SagaUnitOfWorkService;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.workflow.SagaSyncStep;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.workflow.SagaWorkflow;
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.topic.service.TopicService;
import pt.ulisboa.tecnico.socialsoftware.quizzes.sagas.aggregates.dtos.SagaTopicDto;
import pt.ulisboa.tecnico.socialsoftware.quizzes.sagas.aggregates.states.TopicSagaState;

import java.util.ArrayList;
import java.util.Arrays;

public class DeleteTopicFunctionalitySagas extends WorkflowFunctionality {

    private SagaTopicDto topic;

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

        SagaSyncStep getTopicStep = new SagaSyncStep("getTopicStep", () -> {
            SagaTopicDto topicDto = (SagaTopicDto) topicService.getTopicById(topicAggregateId, unitOfWork);
            unitOfWorkService.registerSagaState(topicAggregateId, TopicSagaState.READ_TOPIC, unitOfWork);
            this.setTopic(topicDto);
        });
    
        getTopicStep.registerCompensation(() -> {
            unitOfWorkService.registerSagaState(topicAggregateId, GenericSagaState.NOT_IN_SAGA, unitOfWork);
        }, unitOfWork);
    
        SagaSyncStep deleteTopicStep = new SagaSyncStep("deleteTopicStep", () -> {
            topicService.deleteTopic(topicAggregateId, unitOfWork);
        }, new ArrayList<>(Arrays.asList(getTopicStep)));
    
        workflow.addStep(getTopicStep);
        workflow.addStep(deleteTopicStep);
    }
    public SagaTopicDto getTopic() {
        return topic;
    }

    public void setTopic(SagaTopicDto topic) {
        this.topic = topic;
    }
}