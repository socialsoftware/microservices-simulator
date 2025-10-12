package pt.ulisboa.tecnico.socialsoftware.answers.sagas.coordination.topic;

import java.time.LocalDateTime;
import org.springframework.stereotype.Component;
import pt.ulisboa.tecnico.socialsoftware.ms.coordination.workflow.WorkflowFunctionality;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.workflow.SagaWorkflow;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.unitOfWork.SagaUnitOfWork;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.unitOfWork.SagaUnitOfWorkService;
import pt.ulisboa.tecnico.socialsoftware.answers.microservices.topic.service.TopicService;
import pt.ulisboa.tecnico.socialsoftware.answers.microservices.topic.aggregate.TopicDto;
import pt.ulisboa.tecnico.socialsoftware.answers.sagas.aggregates.dtos.SagaTopicDto;

@Component
public class TopicSagaFunctionality extends WorkflowFunctionality {
private final TopicService topicService;
private final SagaUnitOfWorkService unitOfWorkService;

public TopicSagaFunctionality(TopicService topicService, SagaUnitOfWorkService
unitOfWorkService) {
this.topicService = topicService;
this.unitOfWorkService = unitOfWorkService;
}


}