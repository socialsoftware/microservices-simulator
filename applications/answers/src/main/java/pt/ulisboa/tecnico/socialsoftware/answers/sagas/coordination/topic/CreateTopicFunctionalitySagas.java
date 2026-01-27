package pt.ulisboa.tecnico.socialsoftware.answers.sagas.coordination.topic;

import pt.ulisboa.tecnico.socialsoftware.ms.coordination.workflow.WorkflowFunctionality;
import pt.ulisboa.tecnico.socialsoftware.answers.microservices.topic.service.TopicService;
import pt.ulisboa.tecnico.socialsoftware.answers.shared.dtos.TopicDto;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.unitOfWork.SagaUnitOfWork;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.unitOfWork.SagaUnitOfWorkService;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.workflow.SagaSyncStep;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.workflow.SagaWorkflow;
import pt.ulisboa.tecnico.socialsoftware.answers.coordination.webapi.requestDtos.CreateTopicRequestDto;

public class CreateTopicFunctionalitySagas extends WorkflowFunctionality {
    private TopicDto createdTopicDto;
    private final TopicService topicService;
    private final SagaUnitOfWorkService unitOfWorkService;


    public CreateTopicFunctionalitySagas(SagaUnitOfWork unitOfWork, SagaUnitOfWorkService unitOfWorkService, TopicService topicService, CreateTopicRequestDto createRequest) {
        this.topicService = topicService;
        this.unitOfWorkService = unitOfWorkService;
        this.buildWorkflow(createRequest, unitOfWork);
    }

    public void buildWorkflow(CreateTopicRequestDto createRequest, SagaUnitOfWork unitOfWork) {
        this.workflow = new SagaWorkflow(this, unitOfWorkService, unitOfWork);

        SagaSyncStep createTopicStep = new SagaSyncStep("createTopicStep", () -> {
            TopicDto createdTopicDto = topicService.createTopic(createRequest, unitOfWork);
            setCreatedTopicDto(createdTopicDto);
        });

        workflow.addStep(createTopicStep);

    }

    public TopicDto getCreatedTopicDto() {
        return createdTopicDto;
    }

    public void setCreatedTopicDto(TopicDto createdTopicDto) {
        this.createdTopicDto = createdTopicDto;
    }
}
