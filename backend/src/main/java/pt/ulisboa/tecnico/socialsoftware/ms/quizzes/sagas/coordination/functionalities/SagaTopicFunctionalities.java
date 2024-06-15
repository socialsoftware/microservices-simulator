package pt.ulisboa.tecnico.socialsoftware.ms.quizzes.sagas.coordination.functionalities;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

import pt.ulisboa.tecnico.socialsoftware.ms.quizzes.microservices.topic.aggregate.Topic;
import pt.ulisboa.tecnico.socialsoftware.ms.quizzes.microservices.topic.aggregate.TopicCourse;
import pt.ulisboa.tecnico.socialsoftware.ms.quizzes.microservices.topic.aggregate.TopicDto;
import pt.ulisboa.tecnico.socialsoftware.ms.quizzes.microservices.topic.aggregate.TopicFactory;
import pt.ulisboa.tecnico.socialsoftware.ms.quizzes.microservices.topic.service.TopicFunctionalitiesInterface;
import pt.ulisboa.tecnico.socialsoftware.ms.quizzes.microservices.topic.service.TopicService;
import pt.ulisboa.tecnico.socialsoftware.ms.quizzes.microservices.user.aggregate.User;
import pt.ulisboa.tecnico.socialsoftware.ms.quizzes.sagas.coordination.data.CreateTopicData;
import pt.ulisboa.tecnico.socialsoftware.ms.quizzes.sagas.coordination.data.DeleteTopicData;
import pt.ulisboa.tecnico.socialsoftware.ms.quizzes.sagas.coordination.data.FindTopicsByCourseData;
import pt.ulisboa.tecnico.socialsoftware.ms.quizzes.sagas.coordination.data.UpdateTopicData;
import pt.ulisboa.tecnico.socialsoftware.ms.coordination.workflow.SyncStep;
import pt.ulisboa.tecnico.socialsoftware.ms.domain.aggregate.Aggregate.AggregateState;
import pt.ulisboa.tecnico.socialsoftware.ms.quizzes.microservices.course.aggregate.CourseDto;
import pt.ulisboa.tecnico.socialsoftware.ms.quizzes.microservices.course.service.CourseService;
import pt.ulisboa.tecnico.socialsoftware.ms.quizzes.microservices.exception.TutorException;
import pt.ulisboa.tecnico.socialsoftware.ms.quizzes.microservices.quiz.aggregate.Quiz;
import pt.ulisboa.tecnico.socialsoftware.ms.quizzes.microservices.quiz.aggregate.QuizFactory;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.unityOfWork.SagaUnitOfWork;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.unityOfWork.SagaUnitOfWorkService;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.workflow.SagaWorkflow;

import java.util.List;

import static pt.ulisboa.tecnico.socialsoftware.ms.quizzes.microservices.exception.ErrorMessage.*;

import static pt.ulisboa.tecnico.socialsoftware.ms.domain.aggregate.Aggregate.AggregateState.ACTIVE;
import static pt.ulisboa.tecnico.socialsoftware.ms.domain.aggregate.Aggregate.AggregateState.IN_SAGA;

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
        SagaWorkflow workflow = new SagaWorkflow(data, unitOfWorkService, functionalityName);

        SyncStep findTopicsStep = new SyncStep(() -> {
            List<TopicDto> topics = topicService.findTopicsByCourseId(courseAggregateId, unitOfWork);
            data.setTopics(topics);
        });

        workflow.addStep(findTopicsStep);
        workflow.execute();

        return data.getTopics();
    }

    public TopicDto createTopic(Integer courseAggregateId, TopicDto topicDto) throws Exception {
        String functionalityName = new Throwable().getStackTrace()[0].getMethodName();
        SagaUnitOfWork unitOfWork = unitOfWorkService.createUnitOfWork(functionalityName);
    
        CreateTopicData data = new CreateTopicData();
        SagaWorkflow workflow = new SagaWorkflow(data, unitOfWorkService, functionalityName);
    
        SyncStep checkInputStep = new SyncStep(() -> {
            checkInput(topicDto);
        });
    
        SyncStep getCourseStep = new SyncStep(() -> {
            CourseDto courseDto = courseService.getCourseById(courseAggregateId, unitOfWork);
            TopicCourse course = new TopicCourse(courseDto);
            data.setCourse(course);
        });
    
        SyncStep createTopicStep = new SyncStep(() -> {
            TopicDto createdTopicDto = topicService.createTopic(topicDto, data.getCourse(), unitOfWork);
            data.setCreatedTopicDto(createdTopicDto);
        });
    
        createTopicStep.registerCompensation(() -> {
            Topic topic = (Topic) unitOfWorkService.aggregateLoadAndRegisterRead(data.getCreatedTopicDto().getAggregateId(), unitOfWork);
            topic.remove();
            topic.setState(AggregateState.DELETED);
            unitOfWork.registerChanged(topic);
        }, unitOfWork);
    
        workflow.addStep(checkInputStep);
        workflow.addStep(getCourseStep);
        workflow.addStep(createTopicStep);
    
        workflow.execute();
    
        return data.getCreatedTopicDto();
    }

    public void updateTopic(TopicDto topicDto) throws Exception {
        String functionalityName = new Throwable().getStackTrace()[0].getMethodName();
        SagaUnitOfWork unitOfWork = unitOfWorkService.createUnitOfWork(functionalityName);
    
        UpdateTopicData data = new UpdateTopicData();
        SagaWorkflow workflow = new SagaWorkflow(data, unitOfWorkService, functionalityName);
    
        SyncStep checkInputStep = new SyncStep(() -> {
            checkInput(topicDto);
        });
    
        SyncStep getOldTopicStep = new SyncStep(() -> {
            Topic oldTopic = (Topic) unitOfWorkService.aggregateLoadAndRegisterRead(topicDto.getAggregateId(), unitOfWork);
            oldTopic.setState(AggregateState.IN_SAGA);
            data.setOldTopic(oldTopic);
        });
    
        getOldTopicStep.registerCompensation(() -> {
            Topic newTopic = topicFactory.createTopicFromExisting(data.getOldTopic());
            unitOfWork.registerChanged(newTopic);
            newTopic.setState(AggregateState.ACTIVE);
        }, unitOfWork);
    
        SyncStep updateTopicStep = new SyncStep(() -> {
            topicService.updateTopic(topicDto, unitOfWork);
        });
    
        workflow.addStep(checkInputStep);
        workflow.addStep(getOldTopicStep);
        workflow.addStep(updateTopicStep);
    
        workflow.execute();
    }

    public void deleteTopic(Integer topicAggregateId) throws Exception {
        String functionalityName = new Throwable().getStackTrace()[0].getMethodName();
        SagaUnitOfWork unitOfWork = unitOfWorkService.createUnitOfWork(functionalityName);
    
        DeleteTopicData data = new DeleteTopicData();
        SagaWorkflow workflow = new SagaWorkflow(data, unitOfWorkService, functionalityName);
    
        SyncStep getTopicStep = new SyncStep(() -> {
            Topic topic = (Topic) unitOfWorkService.aggregateLoadAndRegisterRead(topicAggregateId, unitOfWork);
            topic.setState(AggregateState.IN_SAGA);
            data.setTopic(topic);
        });
    
        getTopicStep.registerCompensation(() -> {
            Topic topic = data.getTopic();
            topic.setState(AggregateState.ACTIVE);
            unitOfWork.registerChanged(topic);
        }, unitOfWork);
    
        SyncStep deleteTopicStep = new SyncStep(() -> {
            topicService.deleteTopic(topicAggregateId, unitOfWork);
        });
    
        workflow.addStep(getTopicStep);
        workflow.addStep(deleteTopicStep);
    
        workflow.execute();
    }

    private void checkInput(TopicDto topicDto) {
        if (topicDto.getName() == null) {
            throw new TutorException(TOPIC_MISSING_NAME);
        }
    }
}
