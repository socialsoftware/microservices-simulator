package pt.ulisboa.tecnico.socialsoftware.ms.quizzes.coordination.functionalities;

import static pt.ulisboa.tecnico.socialsoftware.ms.quizzes.microservices.exception.ErrorMessage.TOPIC_MISSING_NAME;

import java.util.Arrays;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;
import pt.ulisboa.tecnico.socialsoftware.ms.causal.unityOfWork.CausalUnitOfWork;
import pt.ulisboa.tecnico.socialsoftware.ms.causal.unityOfWork.CausalUnitOfWorkService;
import pt.ulisboa.tecnico.socialsoftware.ms.quizzes.causal.coordination.CreateTopicFunctionalityTCC;
import pt.ulisboa.tecnico.socialsoftware.ms.quizzes.causal.coordination.DeleteTopicFunctionalityTCC;
import pt.ulisboa.tecnico.socialsoftware.ms.quizzes.causal.coordination.FindTopicsByCourseFunctionalityTCC;
import pt.ulisboa.tecnico.socialsoftware.ms.quizzes.causal.coordination.GetTopicByIdFunctionalityTCC;
import pt.ulisboa.tecnico.socialsoftware.ms.quizzes.causal.coordination.UpdateTopicFunctionalityTCC;
import pt.ulisboa.tecnico.socialsoftware.ms.quizzes.microservices.course.service.CourseService;
import pt.ulisboa.tecnico.socialsoftware.ms.quizzes.microservices.exception.TutorException;
import pt.ulisboa.tecnico.socialsoftware.ms.quizzes.microservices.topic.aggregate.TopicDto;
import pt.ulisboa.tecnico.socialsoftware.ms.quizzes.microservices.topic.aggregate.TopicFactory;
import pt.ulisboa.tecnico.socialsoftware.ms.quizzes.microservices.topic.service.TopicService;
import pt.ulisboa.tecnico.socialsoftware.ms.quizzes.sagas.coordination.workflows.CreateTopicFunctionalitySagas;
import pt.ulisboa.tecnico.socialsoftware.ms.quizzes.sagas.coordination.workflows.DeleteTopicFunctionalitySagas;
import pt.ulisboa.tecnico.socialsoftware.ms.quizzes.sagas.coordination.workflows.FindTopicsByCourseFunctionalitySagas;
import pt.ulisboa.tecnico.socialsoftware.ms.quizzes.sagas.coordination.workflows.GetTopicByIdFunctionalitySagas;
import pt.ulisboa.tecnico.socialsoftware.ms.quizzes.sagas.coordination.workflows.UpdateTopicFunctionalitySagas;
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

    private String workflowType;

    @PostConstruct
    public void init() {
        // Determine the workflow type based on active profiles
        String[] activeProfiles = env.getActiveProfiles();
        if (Arrays.asList(activeProfiles).contains("sagas")) {
            workflowType = "sagas";
        } else if (Arrays.asList(activeProfiles).contains("tcc")) {
            workflowType = "tcc";
        } else {
            workflowType = "unknown"; // Default or fallback value
        }
    }

    public List<TopicDto> findTopicsByCourseAggregateId(Integer courseAggregateId) {
        String functionalityName = new Throwable().getStackTrace()[0].getMethodName();

        if ("sagas".equals(workflowType)) {
            SagaUnitOfWork unitOfWork = sagaUnitOfWorkService.createUnitOfWork(functionalityName);
            FindTopicsByCourseFunctionalitySagas functionality = new FindTopicsByCourseFunctionalitySagas(
                    topicService, sagaUnitOfWorkService, courseAggregateId, unitOfWork);
            functionality.executeWorkflow(unitOfWork);
            return functionality.getTopics();
        } else {
            CausalUnitOfWork unitOfWork = causalUnitOfWorkService.createUnitOfWork(functionalityName);
            FindTopicsByCourseFunctionalityTCC functionality = new FindTopicsByCourseFunctionalityTCC(
                    topicService, causalUnitOfWorkService, courseAggregateId, unitOfWork);
            functionality.executeWorkflow(unitOfWork);
            return functionality.getTopics();
        }
    }

    public TopicDto getTopicByAggregateId(Integer topicAggregateId) {
        String functionalityName = new Throwable().getStackTrace()[0].getMethodName();

        if ("sagas".equals(workflowType)) {
            SagaUnitOfWork unitOfWork = sagaUnitOfWorkService.createUnitOfWork(functionalityName);
            GetTopicByIdFunctionalitySagas functionality = new GetTopicByIdFunctionalitySagas(
                    topicService, sagaUnitOfWorkService, topicAggregateId, unitOfWork);
            functionality.executeWorkflow(unitOfWork);
            return functionality.getTopicDto();
        } else {
            CausalUnitOfWork unitOfWork = causalUnitOfWorkService.createUnitOfWork(functionalityName);
            GetTopicByIdFunctionalityTCC functionality = new GetTopicByIdFunctionalityTCC(
                    topicService, causalUnitOfWorkService, topicAggregateId, unitOfWork);
            functionality.executeWorkflow(unitOfWork);
            return functionality.getTopicDto();
        }
    }

    public TopicDto createTopic(Integer courseAggregateId, TopicDto topicDto) throws Exception {
        String functionalityName = new Throwable().getStackTrace()[0].getMethodName();

        if ("sagas".equals(workflowType)) {
            SagaUnitOfWork unitOfWork = sagaUnitOfWorkService.createUnitOfWork(functionalityName);
            checkInput(topicDto);
            CreateTopicFunctionalitySagas functionality = new CreateTopicFunctionalitySagas(
                    topicService, courseService, sagaUnitOfWorkService, courseAggregateId, topicDto, unitOfWork);
            functionality.executeWorkflow(unitOfWork);
            return functionality.getCreatedTopicDto();
        } else {
            CausalUnitOfWork unitOfWork = causalUnitOfWorkService.createUnitOfWork(functionalityName);
            checkInput(topicDto);
            CreateTopicFunctionalityTCC functionality = new CreateTopicFunctionalityTCC(
                    topicService, courseService, causalUnitOfWorkService, courseAggregateId, topicDto, unitOfWork);
            functionality.executeWorkflow(unitOfWork);
            return functionality.getCreatedTopicDto();
        }
    }

    public void updateTopic(TopicDto topicDto) throws Exception {
        String functionalityName = new Throwable().getStackTrace()[0].getMethodName();

        if ("sagas".equals(workflowType)) {
            SagaUnitOfWork unitOfWork = sagaUnitOfWorkService.createUnitOfWork(functionalityName);
            checkInput(topicDto);
            UpdateTopicFunctionalitySagas functionality = new UpdateTopicFunctionalitySagas(
                    topicService, sagaUnitOfWorkService, topicDto, topicFactory, unitOfWork);
            functionality.executeWorkflow(unitOfWork);
        } else {
            CausalUnitOfWork unitOfWork = causalUnitOfWorkService.createUnitOfWork(functionalityName);
            checkInput(topicDto);
            UpdateTopicFunctionalityTCC functionality = new UpdateTopicFunctionalityTCC(
                    topicService, causalUnitOfWorkService, topicDto, topicFactory, unitOfWork);
            functionality.executeWorkflow(unitOfWork);
        }
    }

    public void deleteTopic(Integer topicAggregateId) throws Exception {
        String functionalityName = new Throwable().getStackTrace()[0].getMethodName();

        if ("sagas".equals(workflowType)) {
            SagaUnitOfWork unitOfWork = sagaUnitOfWorkService.createUnitOfWork(functionalityName);
            DeleteTopicFunctionalitySagas functionality = new DeleteTopicFunctionalitySagas(
                    topicService, sagaUnitOfWorkService, topicAggregateId, unitOfWork);
            functionality.executeWorkflow(unitOfWork);
        } else {
            CausalUnitOfWork unitOfWork = causalUnitOfWorkService.createUnitOfWork(functionalityName);
            DeleteTopicFunctionalityTCC functionality = new DeleteTopicFunctionalityTCC(
                    topicService, causalUnitOfWorkService, topicAggregateId, unitOfWork);
            functionality.executeWorkflow(unitOfWork);
        }
    }

    private void checkInput(TopicDto topicDto) {
        if (topicDto.getName() == null) {
            throw new TutorException(TOPIC_MISSING_NAME);
        }
    }
}
