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

public class UpdateTopicFunctionalitySagas extends WorkflowFunctionality {
    private TopicDto updatedTopicDto;
    private final TopicService topicService;
    private final SagaUnitOfWorkService unitOfWorkService;


    public UpdateTopicFunctionalitySagas(SagaUnitOfWork unitOfWork, SagaUnitOfWorkService unitOfWorkService, TopicService topicService, TopicDto topicDto) {
        this.topicService = topicService;
        this.unitOfWorkService = unitOfWorkService;
        this.buildWorkflow(topicDto, unitOfWork);
    }

    public void buildWorkflow(TopicDto topicDto, SagaUnitOfWork unitOfWork) {
        this.workflow = new SagaWorkflow(this, unitOfWorkService, unitOfWork);

        SagaSyncStep getTopicStep = new SagaSyncStep("getTopicStep", () -> {
            unitOfWorkService.registerSagaState(topicDto.getAggregateId(), TopicSagaState.READ_TOPIC, unitOfWork);
        });

        getTopicStep.registerCompensation(() -> {
            unitOfWorkService.registerSagaState(topicDto.getAggregateId(), GenericSagaState.NOT_IN_SAGA, unitOfWork);
        }, unitOfWork);

        SagaSyncStep updateTopicStep = new SagaSyncStep("updateTopicStep", () -> {
            TopicDto updatedTopicDto = topicService.updateTopic(topicDto.getAggregateId(), topicDto, unitOfWork);
            setUpdatedTopicDto(updatedTopicDto);
        }, new ArrayList<>(Arrays.asList(getTopicStep)));

        workflow.addStep(getTopicStep);
        workflow.addStep(updateTopicStep);

    }

    public TopicDto getUpdatedTopicDto() {
        return updatedTopicDto;
    }

    public void setUpdatedTopicDto(TopicDto updatedTopicDto) {
        this.updatedTopicDto = updatedTopicDto;
    }
}
