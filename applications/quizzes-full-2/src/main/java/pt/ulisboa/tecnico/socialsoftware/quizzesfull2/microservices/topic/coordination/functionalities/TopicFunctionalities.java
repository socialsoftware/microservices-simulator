package pt.ulisboa.tecnico.socialsoftware.quizzesfull2.microservices.topic.coordination.functionalities;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import pt.ulisboa.tecnico.socialsoftware.ms.messaging.CommandGateway;
import pt.ulisboa.tecnico.socialsoftware.ms.transaction.sagas.unitOfWork.SagaUnitOfWork;
import pt.ulisboa.tecnico.socialsoftware.ms.transaction.sagas.unitOfWork.SagaUnitOfWorkService;
import pt.ulisboa.tecnico.socialsoftware.quizzesfull2.microservices.topic.aggregate.TopicDto;
import pt.ulisboa.tecnico.socialsoftware.quizzesfull2.microservices.topic.coordination.sagas.CreateTopicFunctionalitySagas;
import pt.ulisboa.tecnico.socialsoftware.quizzesfull2.microservices.topic.coordination.sagas.DeleteTopicFunctionalitySagas;
import pt.ulisboa.tecnico.socialsoftware.quizzesfull2.microservices.topic.coordination.sagas.GetTopicsByCourseFunctionalitySagas;
import pt.ulisboa.tecnico.socialsoftware.quizzesfull2.microservices.topic.coordination.sagas.UpdateTopicFunctionalitySagas;

import java.util.List;

@Service
public class TopicFunctionalities {
    @Autowired
    private SagaUnitOfWorkService unitOfWorkService;
    @Autowired
    private CommandGateway commandGateway;

    public List<TopicDto> getTopicsByCourse(Integer courseAggregateId) {
        SagaUnitOfWork unitOfWork = unitOfWorkService.createUnitOfWork("getTopicsByCourse");
        GetTopicsByCourseFunctionalitySagas saga = new GetTopicsByCourseFunctionalitySagas(
                unitOfWorkService, courseAggregateId, unitOfWork, commandGateway);
        saga.executeWorkflow(unitOfWork);
        return saga.getTopics();
    }

    public TopicDto createTopic(TopicDto topicDto) {
        SagaUnitOfWork unitOfWork = unitOfWorkService.createUnitOfWork("createTopic");
        CreateTopicFunctionalitySagas saga = new CreateTopicFunctionalitySagas(
                unitOfWorkService, topicDto, unitOfWork, commandGateway);
        saga.executeWorkflow(unitOfWork);
        return saga.getTopicDto();
    }

    public void updateTopic(Integer topicAggregateId, String name) {
        SagaUnitOfWork unitOfWork = unitOfWorkService.createUnitOfWork("updateTopic");
        UpdateTopicFunctionalitySagas saga = new UpdateTopicFunctionalitySagas(
                unitOfWorkService, topicAggregateId, name, unitOfWork, commandGateway);
        saga.executeWorkflow(unitOfWork);
    }

    public void deleteTopic(Integer topicAggregateId) {
        SagaUnitOfWork unitOfWork = unitOfWorkService.createUnitOfWork("deleteTopic");
        DeleteTopicFunctionalitySagas saga = new DeleteTopicFunctionalitySagas(
                unitOfWorkService, topicAggregateId, unitOfWork, commandGateway);
        saga.executeWorkflow(unitOfWork);
    }
}
