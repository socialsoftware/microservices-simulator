package pt.ulisboa.tecnico.socialsoftware.answers.sagas.coordination.topic;

import pt.ulisboa.tecnico.socialsoftware.ms.coordination.workflow.WorkflowFunctionality;
import pt.ulisboa.tecnico.socialsoftware.answers.microservices.topic.service.TopicService;
import pt.ulisboa.tecnico.socialsoftware.answers.shared.dtos.TopicDto;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.unitOfWork.SagaUnitOfWork;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.unitOfWork.SagaUnitOfWorkService;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.workflow.SagaSyncStep;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.workflow.SagaWorkflow;
import java.util.ArrayList;
import java.util.Arrays;
import pt.ulisboa.tecnico.socialsoftware.answers.sagas.aggregates.states.TopicSagaState;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.aggregate.GenericSagaState;

public class DeleteTopicFunctionalitySagas extends WorkflowFunctionality {
    private TopicDto deletedTopicDto;
    private final TopicService topicService;
    private final SagaUnitOfWorkService unitOfWorkService;


    public DeleteTopicFunctionalitySagas(SagaUnitOfWork unitOfWork, SagaUnitOfWorkService unitOfWorkService, TopicService topicService, Integer topicAggregateId) {
        this.topicService = topicService;
        this.unitOfWorkService = unitOfWorkService;
        this.buildWorkflow(topicAggregateId, unitOfWork);
    }

    public void buildWorkflow(Integer topicAggregateId, SagaUnitOfWork unitOfWork) {
        this.workflow = new SagaWorkflow(this, unitOfWorkService, unitOfWork);

        SagaSyncStep getTopicStep = new SagaSyncStep("getTopicStep", () -> {
            TopicDto deletedTopicDto = topicService.getTopicById(topicAggregateId, unitOfWork);
            setDeletedTopicDto(deletedTopicDto);
            unitOfWorkService.registerSagaState(topicAggregateId, TopicSagaState.READ_TOPIC, unitOfWork);
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

    public TopicDto getDeletedTopicDto() {
        return deletedTopicDto;
    }

    public void setDeletedTopicDto(TopicDto deletedTopicDto) {
        this.deletedTopicDto = deletedTopicDto;
    }
}
