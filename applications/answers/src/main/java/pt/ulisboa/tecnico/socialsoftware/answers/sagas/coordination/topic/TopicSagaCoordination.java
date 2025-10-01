package pt.ulisboa.tecnico.socialsoftware.answers.sagas.coordination.topic;

import ${this.getBasePackage()}.ms.coordination.workflow.WorkflowFunctionality;
import ${this.getBasePackage()}.ms.sagas.unitOfWork.SagaUnitOfWork;
import ${this.getBasePackage()}.ms.sagas.unitOfWork.SagaUnitOfWorkService;
import ${this.getBasePackage()}.ms.sagas.workflow.SagaSyncStep;
import ${this.getBasePackage()}.ms.sagas.workflow.SagaWorkflow;
import pt.ulisboa.tecnico.socialsoftware.answers.microservices.topic.service.TopicService;
import pt.ulisboa.tecnico.socialsoftware.answers.microservices.topic.aggregate.TopicDto;
import pt.ulisboa.tecnico.socialsoftware.answers.sagas.aggregates.dtos.SagaTopicDto;
import pt.ulisboa.tecnico.socialsoftware.answers.sagas.aggregates.states.TopicSagaState;
import ${this.getBasePackage()}.ms.sagas.aggregate.GenericSagaState;

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