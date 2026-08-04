package pt.ulisboa.tecnico.socialsoftware.quizzesfull2.microservices.topic.coordination.functionalities;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import pt.ulisboa.tecnico.socialsoftware.ms.messaging.CommandGateway;
import pt.ulisboa.tecnico.socialsoftware.ms.transaction.sagas.unitOfWork.SagaUnitOfWork;
import pt.ulisboa.tecnico.socialsoftware.ms.transaction.sagas.unitOfWork.SagaUnitOfWorkService;
import pt.ulisboa.tecnico.socialsoftware.quizzesfull2.microservices.topic.aggregate.TopicDto;
import pt.ulisboa.tecnico.socialsoftware.quizzesfull2.microservices.topic.coordination.sagas.GetTopicsByCourseFunctionalitySagas;

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
}
