package pt.ulisboa.tecnico.socialsoftware.answers.sagas.coordination.topic;

import pt.ulisboa.tecnico.socialsoftware.ms.coordination.workflow.WorkflowFunctionality;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.unitOfWork.SagaUnitOfWork;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.unitOfWork.SagaUnitOfWorkService;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.workflow.SagaSyncStep;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.workflow.SagaWorkflow;
import pt.ulisboa.tecnico.socialsoftware.answers.microservices.topic.service.TopicService;
import pt.ulisboa.tecnico.socialsoftware.answers.shared.dtos.TopicDto;
import pt.ulisboa.tecnico.socialsoftware.answers.sagas.aggregates.dtos.SagaTopicDto;
import pt.ulisboa.tecnico.socialsoftware.answers.sagas.aggregates.states.TopicSagaState;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.aggregate.GenericSagaState;

public class TopicSagaCoordination extends WorkflowFunctionality {
private TopicDto topicDto;
private SagaTopicDto topic;
private final TopicService topicService;
private final SagaUnitOfWorkService unitOfWorkService;

public TopicSagaCoordination(TopicService topicService, SagaUnitOfWorkService
unitOfWorkService,
TopicDto topicDto, SagaUnitOfWork unitOfWork) {
this.topicService = topicService;
this.unitOfWorkService = unitOfWorkService;
this.buildWorkflow(topicDto, unitOfWork);
}

public void buildWorkflow(TopicDto topicDto, SagaUnitOfWork unitOfWork) {
this.workflow = new SagaWorkflow(this, unitOfWorkService, unitOfWork);
// Saga coordination logic will be implemented here
}

// Getters and setters
public TopicDto getTopicDto() {
return topicDto;
}

public void setTopicDto(TopicDto topicDto) {
this.topicDto = topicDto;
}

public SagaTopicDto getTopic() {
return topic;
}

public void setTopic(SagaTopicDto topic) {
this.topic = topic;
}
}