package pt.ulisboa.tecnico.socialsoftware.ms.quizzes.sagas.coordination.functionalities;

import static pt.ulisboa.tecnico.socialsoftware.ms.quizzes.microservices.exception.ErrorMessage.TOPIC_MISSING_NAME;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

import pt.ulisboa.tecnico.socialsoftware.ms.coordination.workflow.SyncStep;
import pt.ulisboa.tecnico.socialsoftware.ms.quizzes.microservices.course.aggregate.CourseDto;
import pt.ulisboa.tecnico.socialsoftware.ms.quizzes.microservices.course.service.CourseService;
import pt.ulisboa.tecnico.socialsoftware.ms.quizzes.microservices.exception.TutorException;
import pt.ulisboa.tecnico.socialsoftware.ms.quizzes.microservices.topic.aggregate.Topic;
import pt.ulisboa.tecnico.socialsoftware.ms.quizzes.microservices.topic.aggregate.TopicCourse;
import pt.ulisboa.tecnico.socialsoftware.ms.quizzes.microservices.topic.aggregate.TopicDto;
import pt.ulisboa.tecnico.socialsoftware.ms.quizzes.microservices.topic.aggregate.TopicFactory;
import pt.ulisboa.tecnico.socialsoftware.ms.quizzes.microservices.topic.service.TopicFunctionalitiesInterface;
import pt.ulisboa.tecnico.socialsoftware.ms.quizzes.microservices.topic.service.TopicService;
import pt.ulisboa.tecnico.socialsoftware.ms.quizzes.sagas.aggregates.SagaTopic;
import pt.ulisboa.tecnico.socialsoftware.ms.quizzes.sagas.coordination.data.CreateTopicData;
import pt.ulisboa.tecnico.socialsoftware.ms.quizzes.sagas.coordination.data.DeleteTopicData;
import pt.ulisboa.tecnico.socialsoftware.ms.quizzes.sagas.coordination.data.FindTopicsByCourseData;
import pt.ulisboa.tecnico.socialsoftware.ms.quizzes.sagas.coordination.data.GetTopicByIdData;
import pt.ulisboa.tecnico.socialsoftware.ms.quizzes.sagas.coordination.data.UpdateTopicData;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.aggregate.SagaState;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.unityOfWork.SagaUnitOfWork;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.unityOfWork.SagaUnitOfWorkService;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.workflow.SagaWorkflow;

@Profile("sagas")
@Service
public class SagaTopicFunctionalities implements TopicFunctionalitiesInterface {
    @Autowired
    private TopicService topicService;
    @Autowired
    private CourseService courseService;
    @Autowired
    private SagaUnitOfWorkService unitOfWorkService;
    @Autowired
    private TopicFactory topicFactory;

    public List<TopicDto> findTopicsByCourseAggregateId(Integer courseAggregateId) {
        String functionalityName = new Throwable().getStackTrace()[0].getMethodName();
        SagaUnitOfWork unitOfWork = unitOfWorkService.createUnitOfWork(functionalityName);

        FindTopicsByCourseData data = new FindTopicsByCourseData();
        SagaWorkflow workflow = new SagaWorkflow(data, unitOfWorkService, unitOfWork);

        SyncStep findTopicsStep = new SyncStep(() -> {
            List<TopicDto> topics = topicService.findTopicsByCourseId(courseAggregateId, unitOfWork);
            data.setTopics(topics);
        });

        workflow.addStep(findTopicsStep);
        workflow.execute(unitOfWork);

        return data.getTopics();
    }

    public TopicDto getTopicByAggregateId(Integer topicAggregateId) {
        String functionalityName = new Throwable().getStackTrace()[0].getMethodName();
        SagaUnitOfWork unitOfWork = unitOfWorkService.createUnitOfWork(functionalityName);
    
        GetTopicByIdData data = new GetTopicByIdData();
        SagaWorkflow workflow = new SagaWorkflow(data, unitOfWorkService, unitOfWork);
    
        SyncStep getTopicStep = new SyncStep(() -> {
            TopicDto topicDto = topicService.getTopicById(topicAggregateId, unitOfWork);
            data.setTopicDto(topicDto);
        });
    
        workflow.addStep(getTopicStep);
        workflow.execute(unitOfWork);
    
        return data.getTopicDto();
    }

    public TopicDto createTopic(Integer courseAggregateId, TopicDto topicDto) throws Exception {
        String functionalityName = new Throwable().getStackTrace()[0].getMethodName();
        SagaUnitOfWork unitOfWork = unitOfWorkService.createUnitOfWork(functionalityName);
    
        CreateTopicData data = new CreateTopicData();
        SagaWorkflow workflow = new SagaWorkflow(data, unitOfWorkService, unitOfWork);
    
        SyncStep checkInputStep = new SyncStep(() -> {
            checkInput(topicDto);
        });
    
        SyncStep getCourseStep = new SyncStep(() -> {
            CourseDto courseDto = courseService.getCourseById(courseAggregateId, unitOfWork);
            TopicCourse course = new TopicCourse(courseDto);
            data.setCourse(course);
        }, new ArrayList<>(Arrays.asList(checkInputStep)));
    
        SyncStep createTopicStep = new SyncStep(() -> {
            TopicDto createdTopicDto = topicService.createTopic(topicDto, data.getCourse(), unitOfWork);
            data.setCreatedTopicDto(createdTopicDto);
        }, new ArrayList<>(Arrays.asList(getCourseStep)));
    
        createTopicStep.registerCompensation(() -> {
            Topic topic = (Topic) unitOfWorkService.aggregateLoadAndRegisterRead(data.getCreatedTopicDto().getAggregateId(), unitOfWork);
            topic.remove();
            unitOfWork.registerChanged(topic);
        }, unitOfWork);
    
        workflow.addStep(checkInputStep);
        workflow.addStep(getCourseStep);
        workflow.addStep(createTopicStep);
    
        workflow.execute(unitOfWork);
    
        return data.getCreatedTopicDto();
    }

    public void updateTopic(TopicDto topicDto) throws Exception {
        String functionalityName = new Throwable().getStackTrace()[0].getMethodName();
        SagaUnitOfWork unitOfWork = unitOfWorkService.createUnitOfWork(functionalityName);
    
        UpdateTopicData data = new UpdateTopicData();
        SagaWorkflow workflow = new SagaWorkflow(data, unitOfWorkService, unitOfWork);
    
        SyncStep checkInputStep = new SyncStep(() -> {
            checkInput(topicDto);
        });
    
        SyncStep getOldTopicStep = new SyncStep(() -> {
            SagaTopic oldTopic = (SagaTopic) unitOfWorkService.aggregateLoadAndRegisterRead(topicDto.getAggregateId(), unitOfWork);
            unitOfWorkService.registerSagaState(oldTopic, SagaState.UPDATE_TOPIC_READ_TOPIC, unitOfWork);
            data.setOldTopic(oldTopic);
        }, new ArrayList<>(Arrays.asList(checkInputStep)));
    
        getOldTopicStep.registerCompensation(() -> {
            Topic newTopic = topicFactory.createTopicFromExisting(data.getOldTopic());
            unitOfWorkService.registerSagaState((SagaTopic) newTopic, SagaState.NOT_IN_SAGA, unitOfWork);
            unitOfWork.registerChanged(newTopic);
        }, unitOfWork);
    
        SyncStep updateTopicStep = new SyncStep(() -> {
            topicService.updateTopic(topicDto, unitOfWork);
        }, new ArrayList<>(Arrays.asList(getOldTopicStep)));
    
        workflow.addStep(checkInputStep);
        workflow.addStep(getOldTopicStep);
        workflow.addStep(updateTopicStep);
    
        workflow.execute(unitOfWork);
    }

    public void deleteTopic(Integer topicAggregateId) throws Exception {
        String functionalityName = new Throwable().getStackTrace()[0].getMethodName();
        SagaUnitOfWork unitOfWork = unitOfWorkService.createUnitOfWork(functionalityName);
    
        DeleteTopicData data = new DeleteTopicData();
        SagaWorkflow workflow = new SagaWorkflow(data, unitOfWorkService, unitOfWork);
    
        SyncStep getTopicStep = new SyncStep(() -> {
            SagaTopic topic = (SagaTopic) unitOfWorkService.aggregateLoadAndRegisterRead(topicAggregateId, unitOfWork);
            unitOfWorkService.registerSagaState(topic, SagaState.DELETE_TOPIC_READ_TOPIC, unitOfWork);
            data.setTopic(topic);
        });
    
        getTopicStep.registerCompensation(() -> {
            SagaTopic topic = data.getTopic();
            unitOfWorkService.registerSagaState(topic, SagaState.NOT_IN_SAGA, unitOfWork);
            unitOfWork.registerChanged(topic);
        }, unitOfWork);
    
        SyncStep deleteTopicStep = new SyncStep(() -> {
            topicService.deleteTopic(topicAggregateId, unitOfWork);
        }, new ArrayList<>(Arrays.asList(getTopicStep)));
    
        workflow.addStep(getTopicStep);
        workflow.addStep(deleteTopicStep);
    
        workflow.execute(unitOfWork);
    }

    private void checkInput(TopicDto topicDto) {
        if (topicDto.getName() == null) {
            throw new TutorException(TOPIC_MISSING_NAME);
        }
    }
}
