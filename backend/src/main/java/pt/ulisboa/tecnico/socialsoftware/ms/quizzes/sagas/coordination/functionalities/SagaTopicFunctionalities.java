package pt.ulisboa.tecnico.socialsoftware.ms.quizzes.sagas.coordination.functionalities;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;
import pt.ulisboa.tecnico.socialsoftware.ms.quizzes.microservices.topic.aggregate.TopicCourse;
import pt.ulisboa.tecnico.socialsoftware.ms.quizzes.microservices.topic.aggregate.TopicDto;
import pt.ulisboa.tecnico.socialsoftware.ms.quizzes.microservices.topic.service.TopicFunctionalitiesInterface;
import pt.ulisboa.tecnico.socialsoftware.ms.quizzes.microservices.topic.service.TopicService;
import pt.ulisboa.tecnico.socialsoftware.ms.quizzes.microservices.course.aggregate.CourseDto;
import pt.ulisboa.tecnico.socialsoftware.ms.quizzes.microservices.course.service.CourseService;
import pt.ulisboa.tecnico.socialsoftware.ms.quizzes.microservices.exception.TutorException;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.unityOfWork.SagaUnitOfWork;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.unityOfWork.SagaUnitOfWorkService;

import java.util.List;

import static pt.ulisboa.tecnico.socialsoftware.ms.quizzes.microservices.exception.ErrorMessage.*;

@Profile("sagas")
@Service
public class SagaTopicFunctionalities implements TopicFunctionalitiesInterface {
    @Autowired
    private TopicService topicService;
    @Autowired
    private CourseService courseService;
    @Autowired
    private SagaUnitOfWorkService unitOfWorkService;

    public List<TopicDto> findTopicsByCourseAggregateId(Integer courseAggregateId) {
        SagaUnitOfWork unitOfWork = unitOfWorkService.createUnitOfWork(new Throwable().getStackTrace()[0].getMethodName());
        return topicService.findTopicsByCourseId(courseAggregateId, unitOfWork);
    }

    public TopicDto createTopic(Integer courseAggregateId, TopicDto topicDto) throws Exception {
        SagaUnitOfWork unitOfWork = unitOfWorkService.createUnitOfWork(new Throwable().getStackTrace()[0].getMethodName());
        try {
            checkInput(topicDto);
            CourseDto courseDto = courseService.getCourseById(courseAggregateId, unitOfWork);
            TopicCourse course = new TopicCourse(courseDto);
            TopicDto createdTopicDto = topicService.createTopic(topicDto, course, unitOfWork);

            // TODO
            // unitOfWork.registerCompensation(() -> topicService.deleteTopic(createdTopicDto.getAggregateId(), unitOfWork));

            unitOfWorkService.commit(unitOfWork);
            return createdTopicDto;
        } catch (Exception ex) {
            unitOfWorkService.compensate(unitOfWork);
            throw new Exception("Error creating topic", ex);
        }
    }

    public void updateTopic(TopicDto topicDto) throws Exception {
        SagaUnitOfWork unitOfWork = unitOfWorkService.createUnitOfWork(new Throwable().getStackTrace()[0].getMethodName());
        try {
            checkInput(topicDto);
            TopicDto originalTopicDto = topicService.getTopicById(topicDto.getAggregateId(), unitOfWork);

            topicService.updateTopic(topicDto, unitOfWork);

            unitOfWork.registerCompensation(() -> topicService.updateTopic(originalTopicDto, unitOfWork));

            unitOfWorkService.commit(unitOfWork);
        } catch (Exception ex) {
            unitOfWorkService.compensate(unitOfWork);
            throw new Exception("Error updating topic", ex);
        }
    }

    public void deleteTopic(Integer topicAggregateId) throws Exception {
        SagaUnitOfWork unitOfWork = unitOfWorkService.createUnitOfWork(new Throwable().getStackTrace()[0].getMethodName());
        try {
            topicService.deleteTopic(topicAggregateId, unitOfWork);

            // TODO
            // unitOfWork.registerCompensation(() -> topicService.restoreTopic(topicAggregateId, unitOfWork));

            unitOfWorkService.commit(unitOfWork);
        } catch (Exception ex) {
            unitOfWorkService.compensate(unitOfWork);
            throw new Exception("Error deleting topic", ex);
        }
    }

    private void checkInput(TopicDto topicDto) {
        if (topicDto.getName() == null) {
            throw new TutorException(TOPIC_MISSING_NAME);
        }
    }
}
