package pt.ulisboa.tecnico.socialsoftware.ms.quizzes.causal.coordination.functionalities;

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
import pt.ulisboa.tecnico.socialsoftware.ms.causal.unityOfWork.CausalUnitOfWork;
import pt.ulisboa.tecnico.socialsoftware.ms.causal.unityOfWork.CausalUnitOfWorkService;

import java.util.List;

import static pt.ulisboa.tecnico.socialsoftware.ms.quizzes.microservices.exception.ErrorMessage.*;

@Profile("tcc")
@Service
public class CausalTopicFunctionalities implements TopicFunctionalitiesInterface {
    @Autowired
    private TopicService topicService;
    @Autowired
    private CourseService courseService;
    @Autowired
    private CausalUnitOfWorkService unitOfWorkService;

    public List<TopicDto> findTopicsByCourseAggregateId(Integer courseAggregateId) {
        CausalUnitOfWork unitOfWork = unitOfWorkService.createUnitOfWork(new Throwable().getStackTrace()[0].getMethodName());
        return topicService.findTopicsByCourseId(courseAggregateId, unitOfWork);
    }

    public TopicDto createTopic(Integer courseAggregateId, TopicDto topicDto) {
        CausalUnitOfWork unitOfWork = unitOfWorkService.createUnitOfWork(new Throwable().getStackTrace()[0].getMethodName());
        checkInput(topicDto);
        CourseDto courseDto = courseService.getCourseById(courseAggregateId, unitOfWork);
        TopicCourse course = new TopicCourse(courseDto);
        TopicDto topicDto1 = topicService.createTopic(topicDto, course, unitOfWork);
        unitOfWorkService.commit(unitOfWork);
        return topicDto1;
    }

    public void updateTopic(TopicDto topicDto) {
        CausalUnitOfWork unitOfWork = unitOfWorkService.createUnitOfWork(new Throwable().getStackTrace()[0].getMethodName());
        checkInput(topicDto);
        topicService.updateTopic(topicDto, unitOfWork);
        unitOfWorkService.commit(unitOfWork);
    }

    public void deleteTopic(Integer topicAggregateId) {
        CausalUnitOfWork unitOfWork = unitOfWorkService.createUnitOfWork(new Throwable().getStackTrace()[0].getMethodName());
        topicService.deleteTopic(topicAggregateId, unitOfWork);
        unitOfWorkService.commit(unitOfWork);
    }

    private void checkInput(TopicDto topicDto) {
        if (topicDto.getName() == null) {
            throw new TutorException(TOPIC_MISSING_NAME);
        }
    }
}
