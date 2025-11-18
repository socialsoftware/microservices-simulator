package pt.ulisboa.tecnico.socialsoftware.answers.coordination.functionalities;

import static pt.ulisboa.tecnico.socialsoftware.ms.TransactionalModel.SAGAS;
import static pt.ulisboa.tecnico.socialsoftware.answers.microservices.exception.AnswersErrorMessage.*;

import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.ArrayList;
import java.util.HashSet;
import pt.ulisboa.tecnico.socialsoftware.answers.microservices.exception.AnswersException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Service;
import jakarta.annotation.PostConstruct;
import pt.ulisboa.tecnico.socialsoftware.ms.TransactionalModel;
import pt.ulisboa.tecnico.socialsoftware.ms.coordination.unitOfWork.UnitOfWork;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.unitOfWork.SagaUnitOfWork;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.unitOfWork.SagaUnitOfWorkService;
import pt.ulisboa.tecnico.socialsoftware.answers.sagas.coordination.topic.*;
import pt.ulisboa.tecnico.socialsoftware.answers.microservices.topic.service.TopicService;
import pt.ulisboa.tecnico.socialsoftware.answers.microservices.topicfactory.service.TopicFactory;
import pt.ulisboa.tecnico.socialsoftware.answers.shared.dtos.TopicDto;
import pt.ulisboa.tecnico.socialsoftware.answers.shared.dtos.TopicCourseDto;

@Service
public class TopicFunctionalities {
    @Autowired
    private TopicService topicService;

    @Autowired
    private SagaUnitOfWorkService sagaUnitOfWorkService;

    @Autowired
    private TopicFactory topicFactory;


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

    public List<TopicDto> findTopicsByCourseAggregateId(Integer courseAggregateId) {
        String functionalityName = new Throwable().getStackTrace()[0].getMethodName();

        switch (workflowType) {
            case SAGAS:
                SagaUnitOfWork sagaUnitOfWork = sagaUnitOfWorkService.createUnitOfWork(functionalityName);
                FindTopicsByCourseAggregateIdFunctionalitySagas findTopicsByCourseAggregateIdFunctionalitySagas = new FindTopicsByCourseAggregateIdFunctionalitySagas(
                        topicService, sagaUnitOfWorkService, sagaUnitOfWork);
                findTopicsByCourseAggregateIdFunctionalitySagas.executeWorkflow(sagaUnitOfWork);
                return findTopicsByCourseAggregateIdFunctionalitySagas.getFindTopicsByCourseAggregateId();
            default: throw new AnswersException(UNDEFINED_TRANSACTIONAL_MODEL);
        }
    }

    public TopicDto createTopic(Integer courseAggregateId, TopicDto topicDto) throws AnswersException {
        String functionalityName = new Throwable().getStackTrace()[0].getMethodName();

        switch (workflowType) {
            case SAGAS:
                SagaUnitOfWork sagaUnitOfWork = sagaUnitOfWorkService.createUnitOfWork(functionalityName);
                CreateTopicFunctionalitySagas createTopicFunctionalitySagas = new CreateTopicFunctionalitySagas(
                        topicService, sagaUnitOfWorkService, sagaUnitOfWork);
                createTopicFunctionalitySagas.executeWorkflow(sagaUnitOfWork);
                return createTopicFunctionalitySagas.getCreatedTopic();
            default: throw new AnswersException(UNDEFINED_TRANSACTIONAL_MODEL);
        }
    }

    public void updateTopic(TopicDto topicDto) throws AnswersException {
        String functionalityName = new Throwable().getStackTrace()[0].getMethodName();

        switch (workflowType) {
            case SAGAS:
                SagaUnitOfWork sagaUnitOfWork = sagaUnitOfWorkService.createUnitOfWork(functionalityName);
                UpdateTopicFunctionalitySagas updateTopicFunctionalitySagas = new UpdateTopicFunctionalitySagas(
                        topicService, sagaUnitOfWorkService, sagaUnitOfWork);
                updateTopicFunctionalitySagas.executeWorkflow(sagaUnitOfWork);
                break;
            default: throw new AnswersException(UNDEFINED_TRANSACTIONAL_MODEL);
        }
    }

    public void deleteTopic(Integer topicAggregateId) throws AnswersException {
        String functionalityName = new Throwable().getStackTrace()[0].getMethodName();

        switch (workflowType) {
            case SAGAS:
                SagaUnitOfWork sagaUnitOfWork = sagaUnitOfWorkService.createUnitOfWork(functionalityName);
                DeleteTopicFunctionalitySagas deleteTopicFunctionalitySagas = new DeleteTopicFunctionalitySagas(
                        topicService, sagaUnitOfWorkService, sagaUnitOfWork);
                deleteTopicFunctionalitySagas.executeWorkflow(sagaUnitOfWork);
                break;
            default: throw new AnswersException(UNDEFINED_TRANSACTIONAL_MODEL);
        }
    }

}