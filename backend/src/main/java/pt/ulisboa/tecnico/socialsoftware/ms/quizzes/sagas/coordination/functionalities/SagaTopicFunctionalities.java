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
import pt.ulisboa.tecnico.socialsoftware.ms.quizzes.sagas.coordination.functionalitiesWorkflows.CreateTopicFunctionality;
import pt.ulisboa.tecnico.socialsoftware.ms.quizzes.sagas.coordination.functionalitiesWorkflows.DeleteTopicFunctionality;
import pt.ulisboa.tecnico.socialsoftware.ms.quizzes.sagas.coordination.functionalitiesWorkflows.FindTopicsByCourseFunctionality;
import pt.ulisboa.tecnico.socialsoftware.ms.quizzes.sagas.coordination.functionalitiesWorkflows.GetTopicByIdFunctionality;
import pt.ulisboa.tecnico.socialsoftware.ms.quizzes.sagas.coordination.functionalitiesWorkflows.UpdateTopicFunctionality;
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

        FindTopicsByCourseFunctionality functionality = new FindTopicsByCourseFunctionality(topicService, unitOfWorkService, courseAggregateId, unitOfWork);
        
        functionality.executeWorkflow(unitOfWork);
        return functionality.getTopics();
    }

    public TopicDto getTopicByAggregateId(Integer topicAggregateId) {
        String functionalityName = new Throwable().getStackTrace()[0].getMethodName();
        SagaUnitOfWork unitOfWork = unitOfWorkService.createUnitOfWork(functionalityName);
    
        GetTopicByIdFunctionality functionality = new GetTopicByIdFunctionality(topicService, unitOfWorkService, topicAggregateId, unitOfWork);
        
        functionality.executeWorkflow(unitOfWork);
        return functionality.getTopicDto();
    }

    public TopicDto createTopic(Integer courseAggregateId, TopicDto topicDto) throws Exception {
        String functionalityName = new Throwable().getStackTrace()[0].getMethodName();
        SagaUnitOfWork unitOfWork = unitOfWorkService.createUnitOfWork(functionalityName);
    
        checkInput(topicDto);

        CreateTopicFunctionality functionality = new CreateTopicFunctionality(topicService, courseService, unitOfWorkService, courseAggregateId, topicDto, unitOfWork);
        
        functionality.executeWorkflow(unitOfWork);
        return functionality.getCreatedTopicDto();
    }

    public void updateTopic(TopicDto topicDto) throws Exception {
        String functionalityName = new Throwable().getStackTrace()[0].getMethodName();
        SagaUnitOfWork unitOfWork = unitOfWorkService.createUnitOfWork(functionalityName);
    
        checkInput(topicDto);

        UpdateTopicFunctionality functionality = new UpdateTopicFunctionality(topicService, unitOfWorkService, topicDto, topicFactory, unitOfWork);

        functionality.executeWorkflow(unitOfWork);
    }

    public void deleteTopic(Integer topicAggregateId) throws Exception {
        String functionalityName = new Throwable().getStackTrace()[0].getMethodName();
        SagaUnitOfWork unitOfWork = unitOfWorkService.createUnitOfWork(functionalityName);
    
        DeleteTopicFunctionality functionality = new DeleteTopicFunctionality(topicService, unitOfWorkService, topicAggregateId, unitOfWork);
        
        functionality.executeWorkflow(unitOfWork);
    }

    private void checkInput(TopicDto topicDto) {
        if (topicDto.getName() == null) {
            throw new TutorException(TOPIC_MISSING_NAME);
        }
    }
}
