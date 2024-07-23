package pt.ulisboa.tecnico.socialsoftware.ms.quizzes.sagas.coordination.functionalities;

import static pt.ulisboa.tecnico.socialsoftware.ms.quizzes.microservices.exception.ErrorMessage.TOPIC_MISSING_NAME;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

import pt.ulisboa.tecnico.socialsoftware.ms.quizzes.microservices.course.service.CourseService;
import pt.ulisboa.tecnico.socialsoftware.ms.quizzes.microservices.exception.TutorException;
import pt.ulisboa.tecnico.socialsoftware.ms.quizzes.microservices.topic.aggregate.TopicDto;
import pt.ulisboa.tecnico.socialsoftware.ms.quizzes.microservices.topic.aggregate.TopicFactory;
import pt.ulisboa.tecnico.socialsoftware.ms.quizzes.microservices.topic.service.TopicFunctionalitiesInterface;
import pt.ulisboa.tecnico.socialsoftware.ms.quizzes.microservices.topic.service.TopicService;
import pt.ulisboa.tecnico.socialsoftware.ms.quizzes.sagas.coordination.data.CreateTopicData;
import pt.ulisboa.tecnico.socialsoftware.ms.quizzes.sagas.coordination.data.DeleteTopicData;
import pt.ulisboa.tecnico.socialsoftware.ms.quizzes.sagas.coordination.data.FindTopicsByCourseData;
import pt.ulisboa.tecnico.socialsoftware.ms.quizzes.sagas.coordination.data.GetTopicByIdData;
import pt.ulisboa.tecnico.socialsoftware.ms.quizzes.sagas.coordination.data.UpdateTopicData;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.unityOfWork.SagaUnitOfWork;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.unityOfWork.SagaUnitOfWorkService;

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

        FindTopicsByCourseData data = new FindTopicsByCourseData(topicService, unitOfWorkService, courseAggregateId, unitOfWork);
        
        data.executeWorkflow(unitOfWork);
        return data.getTopics();
    }

    public TopicDto getTopicByAggregateId(Integer topicAggregateId) {
        String functionalityName = new Throwable().getStackTrace()[0].getMethodName();
        SagaUnitOfWork unitOfWork = unitOfWorkService.createUnitOfWork(functionalityName);
    
        GetTopicByIdData data = new GetTopicByIdData(topicService, unitOfWorkService, topicAggregateId, unitOfWork);
        
        data.executeWorkflow(unitOfWork);
        return data.getTopicDto();
    }

    public TopicDto createTopic(Integer courseAggregateId, TopicDto topicDto) throws Exception {
        String functionalityName = new Throwable().getStackTrace()[0].getMethodName();
        SagaUnitOfWork unitOfWork = unitOfWorkService.createUnitOfWork(functionalityName);
    
        checkInput(topicDto);

        CreateTopicData data = new CreateTopicData(topicService, courseService, unitOfWorkService, courseAggregateId, topicDto, unitOfWork);
        
        data.executeWorkflow(unitOfWork);
        return data.getCreatedTopicDto();
    }

    public void updateTopic(TopicDto topicDto) throws Exception {
        String functionalityName = new Throwable().getStackTrace()[0].getMethodName();
        SagaUnitOfWork unitOfWork = unitOfWorkService.createUnitOfWork(functionalityName);
    
        checkInput(topicDto);

        UpdateTopicData data = new UpdateTopicData(topicService, unitOfWorkService, topicDto, topicFactory, unitOfWork);

        data.executeWorkflow(unitOfWork);
    }

    public void deleteTopic(Integer topicAggregateId) throws Exception {
        String functionalityName = new Throwable().getStackTrace()[0].getMethodName();
        SagaUnitOfWork unitOfWork = unitOfWorkService.createUnitOfWork(functionalityName);
    
        DeleteTopicData data = new DeleteTopicData(topicService, unitOfWorkService, topicAggregateId, unitOfWork);
        
        data.executeWorkflow(unitOfWork);
    }

    private void checkInput(TopicDto topicDto) {
        if (topicDto.getName() == null) {
            throw new TutorException(TOPIC_MISSING_NAME);
        }
    }
}
