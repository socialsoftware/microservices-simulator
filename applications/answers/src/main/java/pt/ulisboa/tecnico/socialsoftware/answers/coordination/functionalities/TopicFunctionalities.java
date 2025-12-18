package pt.ulisboa.tecnico.socialsoftware.answers.coordination.functionalities;

import static pt.ulisboa.tecnico.socialsoftware.ms.TransactionalModel.SAGAS;
import static pt.ulisboa.tecnico.socialsoftware.answers.microservices.exception.AnswersErrorMessage.*;

import java.util.Arrays;
import pt.ulisboa.tecnico.socialsoftware.answers.microservices.exception.AnswersException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Service;
import jakarta.annotation.PostConstruct;
import pt.ulisboa.tecnico.socialsoftware.ms.TransactionalModel;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.unitOfWork.SagaUnitOfWork;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.unitOfWork.SagaUnitOfWorkService;
import pt.ulisboa.tecnico.socialsoftware.answers.sagas.coordination.topic.*;
import pt.ulisboa.tecnico.socialsoftware.answers.microservices.topic.service.TopicService;
import pt.ulisboa.tecnico.socialsoftware.answers.microservices.course.service.CourseService;
import pt.ulisboa.tecnico.socialsoftware.answers.shared.dtos.TopicDto;
import java.util.List;

@Service
public class TopicFunctionalities {
    @Autowired
    private TopicService topicService;

    @Autowired
    private CourseService courseService;

    @Autowired
    private SagaUnitOfWorkService sagaUnitOfWorkService;


    @Autowired
    private Environment env;

    private TransactionalModel workflowType;

    @PostConstruct
    public void init() {
        String[] activeProfiles = env.getActiveProfiles();
        if (Arrays.asList(activeProfiles).contains(SAGAS.getValue())) {
            workflowType = SAGAS;
        } else {
            throw new AnswersException(UNDEFINED_TRANSACTIONAL_MODEL);
        }
    }

    public TopicDto createTopic(Integer courseAggregateId, TopicDto topicDto) {
        String functionalityName = new Throwable().getStackTrace()[0].getMethodName();

        switch (workflowType) {
            case SAGAS:
                SagaUnitOfWork sagaUnitOfWork = sagaUnitOfWorkService.createUnitOfWork(functionalityName);
                checkInput(topicDto);
                CreateTopicFunctionalitySagas createTopicFunctionalitySagas = new CreateTopicFunctionalitySagas(
                        sagaUnitOfWork, sagaUnitOfWorkService, topicService, courseService, courseAggregateId, topicDto);
                createTopicFunctionalitySagas.executeWorkflow(sagaUnitOfWork);
                return createTopicFunctionalitySagas.getCreatedTopicDto();
            default: throw new AnswersException(UNDEFINED_TRANSACTIONAL_MODEL);
        }
    }

    public TopicDto getTopicById(Integer topicAggregateId) {
        String functionalityName = new Throwable().getStackTrace()[0].getMethodName();

        switch (workflowType) {
            case SAGAS:
                SagaUnitOfWork sagaUnitOfWork = sagaUnitOfWorkService.createUnitOfWork(functionalityName);
                GetTopicByIdFunctionalitySagas getTopicByIdFunctionalitySagas = new GetTopicByIdFunctionalitySagas(
                        sagaUnitOfWork, sagaUnitOfWorkService, topicService, topicAggregateId);
                getTopicByIdFunctionalitySagas.executeWorkflow(sagaUnitOfWork);
                return getTopicByIdFunctionalitySagas.getTopicDto();
            default: throw new AnswersException(UNDEFINED_TRANSACTIONAL_MODEL);
        }
    }

    public TopicDto updateTopic(TopicDto topicDto) {
        String functionalityName = new Throwable().getStackTrace()[0].getMethodName();

        switch (workflowType) {
            case SAGAS:
                SagaUnitOfWork sagaUnitOfWork = sagaUnitOfWorkService.createUnitOfWork(functionalityName);
                checkInput(topicDto);
                UpdateTopicFunctionalitySagas updateTopicFunctionalitySagas = new UpdateTopicFunctionalitySagas(
                        sagaUnitOfWork, sagaUnitOfWorkService, topicService, topicDto);
                updateTopicFunctionalitySagas.executeWorkflow(sagaUnitOfWork);
                return updateTopicFunctionalitySagas.getUpdatedTopicDto();
            default: throw new AnswersException(UNDEFINED_TRANSACTIONAL_MODEL);
        }
    }

    public void deleteTopic(Integer topicAggregateId) {
        String functionalityName = new Throwable().getStackTrace()[0].getMethodName();

        switch (workflowType) {
            case SAGAS:
                SagaUnitOfWork sagaUnitOfWork = sagaUnitOfWorkService.createUnitOfWork(functionalityName);
                DeleteTopicFunctionalitySagas deleteTopicFunctionalitySagas = new DeleteTopicFunctionalitySagas(
                        sagaUnitOfWork, sagaUnitOfWorkService, topicService, topicAggregateId);
                deleteTopicFunctionalitySagas.executeWorkflow(sagaUnitOfWork);
                break;
            default: throw new AnswersException(UNDEFINED_TRANSACTIONAL_MODEL);
        }
    }

    public List<TopicDto> searchTopics(String name, Integer courseAggregateId) {
        String functionalityName = new Throwable().getStackTrace()[0].getMethodName();

        switch (workflowType) {
            case SAGAS:
                SagaUnitOfWork sagaUnitOfWork = sagaUnitOfWorkService.createUnitOfWork(functionalityName);
                SearchTopicsFunctionalitySagas searchTopicsFunctionalitySagas = new SearchTopicsFunctionalitySagas(
                        sagaUnitOfWork, sagaUnitOfWorkService, topicService, name, courseAggregateId);
                searchTopicsFunctionalitySagas.executeWorkflow(sagaUnitOfWork);
                return searchTopicsFunctionalitySagas.getSearchedTopicDtos();
            default: throw new AnswersException(UNDEFINED_TRANSACTIONAL_MODEL);
        }
    }

        private void checkInput(TopicDto topicDto) {
        if (topicDto.getName() == null) {
            throw new AnswersException(TOPIC_MISSING_NAME);
        }
    }
}