package pt.ulisboa.tecnico.socialsoftware.quizzes.sagas.coordination.topic;

import java.util.ArrayList;
import java.util.Arrays;

import pt.ulisboa.tecnico.socialsoftware.ms.coordination.workflow.WorkflowFunctionality;
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.topic.aggregate.TopicDto;
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.topic.aggregate.TopicFactory;
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.topic.service.TopicService;
import pt.ulisboa.tecnico.socialsoftware.quizzes.sagas.aggregates.dtos.SagaTopicDto;
import pt.ulisboa.tecnico.socialsoftware.quizzes.sagas.aggregates.states.TopicSagaState;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.aggregate.GenericSagaState;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.unitOfWork.SagaUnitOfWork;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.unitOfWork.SagaUnitOfWorkService;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.workflow.SagaSyncStep;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.workflow.SagaWorkflow;

public class UpdateTopicFunctionalitySagas extends WorkflowFunctionality {
    private SagaTopicDto topic;
    private final TopicService topicService;
    private final SagaUnitOfWorkService unitOfWorkService;

    public UpdateTopicFunctionalitySagas(TopicService topicService, SagaUnitOfWorkService unitOfWorkService,  
                            TopicDto topicDto, TopicFactory topicFactory, SagaUnitOfWork unitOfWork) {
        this.topicService = topicService;
        this.unitOfWorkService = unitOfWorkService;
        this.buildWorkflow(topicDto, topicFactory, unitOfWork);
    }

    public void buildWorkflow(TopicDto topicDto, TopicFactory topicFactory, SagaUnitOfWork unitOfWork) {
        this.workflow = new SagaWorkflow(this, unitOfWorkService, unitOfWork);

        SagaSyncStep getTopicStep = new SagaSyncStep("getTopicStep", () -> {
            SagaTopicDto topic = (SagaTopicDto) topicService.getTopicById(topicDto.getAggregateId(), unitOfWork);
            unitOfWorkService.registerSagaState(topic.getAggregateId(), TopicSagaState.READ_TOPIC, unitOfWork);
            this.setTopic(topic);
        });
    
        getTopicStep.registerCompensation(() -> {
            unitOfWorkService.registerSagaState(topic.getAggregateId(), GenericSagaState.NOT_IN_SAGA, unitOfWork);
        }, unitOfWork);
    
        SagaSyncStep updateTopicStep = new SagaSyncStep("updateTopicStep", () -> {
            topicService.updateTopic(topicDto, unitOfWork);
        }, new ArrayList<>(Arrays.asList(getTopicStep)));
    
        workflow.addStep(getTopicStep);
        workflow.addStep(updateTopicStep);
    }
    

    public SagaTopicDto getTopic() {
        return topic;
    }

    public void setTopic(SagaTopicDto topic) {
        this.topic = topic;
    }
}