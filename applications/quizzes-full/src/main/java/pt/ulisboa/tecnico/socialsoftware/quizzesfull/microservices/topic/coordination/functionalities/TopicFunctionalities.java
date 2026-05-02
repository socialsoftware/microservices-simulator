package pt.ulisboa.tecnico.socialsoftware.quizzesfull.microservices.topic.coordination.functionalities;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import pt.ulisboa.tecnico.socialsoftware.ms.messaging.CommandGateway;
import pt.ulisboa.tecnico.socialsoftware.ms.transaction.sagas.unitOfWork.SagaUnitOfWork;
import pt.ulisboa.tecnico.socialsoftware.ms.transaction.sagas.unitOfWork.SagaUnitOfWorkService;
import pt.ulisboa.tecnico.socialsoftware.quizzesfull.microservices.topic.aggregate.TopicDto;
import pt.ulisboa.tecnico.socialsoftware.quizzesfull.microservices.exception.QuizzesFullException;
import pt.ulisboa.tecnico.socialsoftware.quizzesfull.microservices.topic.coordination.sagas.CreateTopicFunctionalitySagas;
import pt.ulisboa.tecnico.socialsoftware.quizzesfull.microservices.topic.coordination.sagas.DeleteTopicFunctionalitySagas;
import pt.ulisboa.tecnico.socialsoftware.quizzesfull.microservices.topic.coordination.sagas.GetTopicByIdFunctionalitySagas;
import pt.ulisboa.tecnico.socialsoftware.quizzesfull.microservices.topic.coordination.sagas.GetTopicsByCourseIdFunctionalitySagas;
import pt.ulisboa.tecnico.socialsoftware.quizzesfull.microservices.topic.coordination.sagas.UpdateTopicFunctionalitySagas;

import java.util.List;

import static pt.ulisboa.tecnico.socialsoftware.quizzesfull.microservices.exception.QuizzesFullErrorMessage.TOPIC_MISSING_NAME;

@Service
public class TopicFunctionalities {

    @Autowired
    private SagaUnitOfWorkService unitOfWorkService;

    @Autowired
    private CommandGateway commandGateway;

    public TopicDto createTopic(Integer courseAggregateId, TopicDto topicDto) {
        String functionalityName = new Throwable().getStackTrace()[0].getMethodName();
        SagaUnitOfWork unitOfWork = unitOfWorkService.createUnitOfWork(functionalityName);
        CreateTopicFunctionalitySagas saga = new CreateTopicFunctionalitySagas(
                unitOfWorkService, courseAggregateId, topicDto, unitOfWork, commandGateway);
        saga.executeWorkflow(unitOfWork);
        return saga.getCreatedTopicDto();
    }

    public void updateTopic(TopicDto topicDto) {
        if (topicDto.getName() == null) {
            throw new QuizzesFullException(TOPIC_MISSING_NAME);
        }
        String functionalityName = new Throwable().getStackTrace()[0].getMethodName();
        SagaUnitOfWork unitOfWork = unitOfWorkService.createUnitOfWork(functionalityName);
        UpdateTopicFunctionalitySagas saga = new UpdateTopicFunctionalitySagas(
                unitOfWorkService, topicDto, unitOfWork, commandGateway);
        saga.executeWorkflow(unitOfWork);
    }

    public void deleteTopic(Integer topicAggregateId) {
        String functionalityName = new Throwable().getStackTrace()[0].getMethodName();
        SagaUnitOfWork unitOfWork = unitOfWorkService.createUnitOfWork(functionalityName);
        DeleteTopicFunctionalitySagas saga = new DeleteTopicFunctionalitySagas(
                unitOfWorkService, topicAggregateId, unitOfWork, commandGateway);
        saga.executeWorkflow(unitOfWork);
    }

    public TopicDto getTopicById(Integer topicAggregateId) {
        String functionalityName = new Throwable().getStackTrace()[0].getMethodName();
        SagaUnitOfWork unitOfWork = unitOfWorkService.createUnitOfWork(functionalityName);
        GetTopicByIdFunctionalitySagas saga = new GetTopicByIdFunctionalitySagas(
                unitOfWorkService, topicAggregateId, unitOfWork, commandGateway);
        saga.executeWorkflow(unitOfWork);
        return saga.getTopicDto();
    }

    public List<TopicDto> getTopicsByCourseId(Integer courseAggregateId) {
        String functionalityName = new Throwable().getStackTrace()[0].getMethodName();
        SagaUnitOfWork unitOfWork = unitOfWorkService.createUnitOfWork(functionalityName);
        GetTopicsByCourseIdFunctionalitySagas saga = new GetTopicsByCourseIdFunctionalitySagas(
                unitOfWorkService, courseAggregateId, unitOfWork, commandGateway);
        saga.executeWorkflow(unitOfWork);
        return saga.getTopics();
    }
}
