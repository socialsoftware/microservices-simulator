package pt.ulisboa.tecnico.socialsoftware.quizzes.coordination.functionalities;

import static pt.ulisboa.tecnico.socialsoftware.ms.TransactionalModel.SAGAS;
import static pt.ulisboa.tecnico.socialsoftware.ms.TransactionalModel.TCC;
import static pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.exception.ErrorMessage.TOPIC_MISSING_NAME;
import static pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.exception.ErrorMessage.UNDEFINED_TRANSACTIONAL_MODEL;

import java.util.Arrays;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;
import pt.ulisboa.tecnico.socialsoftware.ms.TransactionalModel;
import pt.ulisboa.tecnico.socialsoftware.ms.causal.unityOfWork.CausalUnitOfWork;
import pt.ulisboa.tecnico.socialsoftware.ms.causal.unityOfWork.CausalUnitOfWorkService;
import pt.ulisboa.tecnico.socialsoftware.quizzes.causal.coordination.topic.*;
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.course.service.CourseService;
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.exception.TutorException;
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.topic.aggregate.TopicDto;
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.topic.aggregate.TopicFactory;
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.topic.service.TopicService;
import pt.ulisboa.tecnico.socialsoftware.quizzes.sagas.coordination.topic.*;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.unityOfWork.SagaUnitOfWork;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.unityOfWork.SagaUnitOfWorkService;

@Service
public class TopicFunctionalities {
    @Autowired
    private TopicService topicService;
    @Autowired
    private CourseService courseService;
    @Autowired(required = false)
    private SagaUnitOfWorkService sagaUnitOfWorkService;
    @Autowired(required = false)
    private CausalUnitOfWorkService causalUnitOfWorkService;
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
        } else if (Arrays.asList(activeProfiles).contains(TCC.getValue())) {
            workflowType = TCC;
        } else {
            throw new TutorException(UNDEFINED_TRANSACTIONAL_MODEL);
        }
    }

    public List<TopicDto> findTopicsByCourseAggregateId(Integer courseAggregateId) {
        String functionalityName = new Throwable().getStackTrace()[0].getMethodName();

        switch (workflowType) {
            case SAGAS:
                SagaUnitOfWork sagaUnitOfWork = sagaUnitOfWorkService.createUnitOfWork(functionalityName);
                FindTopicsByCourseFunctionalitySagas findTopicsByCourseFunctionalitySagas = new FindTopicsByCourseFunctionalitySagas(
                        topicService, sagaUnitOfWorkService, courseAggregateId, sagaUnitOfWork);
                findTopicsByCourseFunctionalitySagas.executeWorkflow(sagaUnitOfWork);
                return findTopicsByCourseFunctionalitySagas.getTopics();
            case TCC:
                CausalUnitOfWork causalUnitOfWork = causalUnitOfWorkService.createUnitOfWork(functionalityName);
                FindTopicsByCourseFunctionalityTCC findTopicsByCourseFunctionalityTCC = new FindTopicsByCourseFunctionalityTCC(
                        topicService, causalUnitOfWorkService, courseAggregateId, causalUnitOfWork);
                findTopicsByCourseFunctionalityTCC.executeWorkflow(causalUnitOfWork);
                return findTopicsByCourseFunctionalityTCC.getTopics();
            default: throw new TutorException(UNDEFINED_TRANSACTIONAL_MODEL);
        }
    }

    public TopicDto getTopicByAggregateId(Integer topicAggregateId) {
        String functionalityName = new Throwable().getStackTrace()[0].getMethodName();

        switch (workflowType) {
            case SAGAS:
                SagaUnitOfWork sagaUnitOfWork = sagaUnitOfWorkService.createUnitOfWork(functionalityName);
                GetTopicByIdFunctionalitySagas getTopicByIdFunctionalitySagas = new GetTopicByIdFunctionalitySagas(
                        topicService, sagaUnitOfWorkService, topicAggregateId, sagaUnitOfWork);
                getTopicByIdFunctionalitySagas.executeWorkflow(sagaUnitOfWork);
                return getTopicByIdFunctionalitySagas.getTopicDto();
            case TCC:
                CausalUnitOfWork causalUnitOfWork = causalUnitOfWorkService.createUnitOfWork(functionalityName);
                GetTopicByIdFunctionalityTCC getTopicByIdFunctionalityTCC = new GetTopicByIdFunctionalityTCC(
                        topicService, causalUnitOfWorkService, topicAggregateId, causalUnitOfWork);
                getTopicByIdFunctionalityTCC.executeWorkflow(causalUnitOfWork);
                return getTopicByIdFunctionalityTCC.getTopicDto();
            default: throw new TutorException(UNDEFINED_TRANSACTIONAL_MODEL);
        }
    }

    public TopicDto createTopic(Integer courseAggregateId, TopicDto topicDto) throws TutorException {
        String functionalityName = new Throwable().getStackTrace()[0].getMethodName();

        switch (workflowType) {
            case SAGAS:
                SagaUnitOfWork sagaUnitOfWork = sagaUnitOfWorkService.createUnitOfWork(functionalityName);
                checkInput(topicDto);
                CreateTopicFunctionalitySagas createTopicFunctionalitySagas = new CreateTopicFunctionalitySagas(
                        topicService, courseService, sagaUnitOfWorkService, courseAggregateId, topicDto, sagaUnitOfWork);
                createTopicFunctionalitySagas.executeWorkflow(sagaUnitOfWork);
                return createTopicFunctionalitySagas.getCreatedTopicDto();
            case TCC:
                CausalUnitOfWork causalUnitOfWork = causalUnitOfWorkService.createUnitOfWork(functionalityName);
                checkInput(topicDto);
                CreateTopicFunctionalityTCC createTopicFunctionalityTCC = new CreateTopicFunctionalityTCC(
                        topicService, courseService, causalUnitOfWorkService, courseAggregateId, topicDto, causalUnitOfWork);
                createTopicFunctionalityTCC.executeWorkflow(causalUnitOfWork);
                return createTopicFunctionalityTCC.getCreatedTopicDto();
            default: throw new TutorException(UNDEFINED_TRANSACTIONAL_MODEL);
        }
    }

    public void updateTopic(TopicDto topicDto) {
        String functionalityName = new Throwable().getStackTrace()[0].getMethodName();

        switch (workflowType) {
            case SAGAS:
                SagaUnitOfWork sagaUnitOfWork = sagaUnitOfWorkService.createUnitOfWork(functionalityName);
                checkInput(topicDto);
                UpdateTopicFunctionalitySagas updateTopicFunctionalitySagas = new UpdateTopicFunctionalitySagas(
                        topicService, sagaUnitOfWorkService, topicDto, topicFactory, sagaUnitOfWork);
                updateTopicFunctionalitySagas.executeWorkflow(sagaUnitOfWork);
                break;
            case TCC:
                CausalUnitOfWork causalUnitOfWork = causalUnitOfWorkService.createUnitOfWork(functionalityName);
                checkInput(topicDto);
                UpdateTopicFunctionalityTCC updateTopicFunctionalityTCC = new UpdateTopicFunctionalityTCC(
                        topicService, causalUnitOfWorkService, topicDto, topicFactory, causalUnitOfWork);
                updateTopicFunctionalityTCC.executeWorkflow(causalUnitOfWork);
                break;
            default: throw new TutorException(UNDEFINED_TRANSACTIONAL_MODEL);
        }
    }

    public void deleteTopic(Integer topicAggregateId) {
        String functionalityName = new Throwable().getStackTrace()[0].getMethodName();

        switch (workflowType) {
            case SAGAS:
                SagaUnitOfWork sagaUnitOfWork = sagaUnitOfWorkService.createUnitOfWork(functionalityName);
                DeleteTopicFunctionalitySagas deleteTopicFunctionalitySagas = new DeleteTopicFunctionalitySagas(
                        topicService, sagaUnitOfWorkService, topicAggregateId, sagaUnitOfWork);
                deleteTopicFunctionalitySagas.executeWorkflow(sagaUnitOfWork);
                break;
            case TCC:
                CausalUnitOfWork causalUnitOfWork = causalUnitOfWorkService.createUnitOfWork(functionalityName);
                DeleteTopicFunctionalityTCC deleteTopicFunctionalityTCC = new DeleteTopicFunctionalityTCC(
                        topicService, causalUnitOfWorkService, topicAggregateId, causalUnitOfWork);
                deleteTopicFunctionalityTCC.executeWorkflow(causalUnitOfWork);
                break;
            default: throw new TutorException(UNDEFINED_TRANSACTIONAL_MODEL);
        }
    }

    private void checkInput(TopicDto topicDto) {
        if (topicDto.getName() == null) {
            throw new TutorException(TOPIC_MISSING_NAME);
        }
    }
}
