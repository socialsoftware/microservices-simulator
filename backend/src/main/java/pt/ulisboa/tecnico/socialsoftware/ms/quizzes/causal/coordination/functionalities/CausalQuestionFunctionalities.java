package pt.ulisboa.tecnico.socialsoftware.ms.quizzes.causal.coordination.functionalities;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;
import pt.ulisboa.tecnico.socialsoftware.ms.quizzes.microservices.question.aggregate.QuestionCourse;
import pt.ulisboa.tecnico.socialsoftware.ms.quizzes.microservices.question.aggregate.QuestionTopic;
import pt.ulisboa.tecnico.socialsoftware.ms.quizzes.microservices.question.aggregate.QuestionDto;
import pt.ulisboa.tecnico.socialsoftware.ms.quizzes.microservices.topic.aggregate.TopicDto;
import pt.ulisboa.tecnico.socialsoftware.ms.quizzes.microservices.topic.service.TopicService;
import pt.ulisboa.tecnico.socialsoftware.ms.quizzes.microservices.course.service.CourseService;
import pt.ulisboa.tecnico.socialsoftware.ms.quizzes.microservices.exception.ErrorMessage;
import pt.ulisboa.tecnico.socialsoftware.ms.quizzes.microservices.exception.TutorException;
import pt.ulisboa.tecnico.socialsoftware.ms.quizzes.microservices.question.service.QuestionFunctionalitiesInterface;
import pt.ulisboa.tecnico.socialsoftware.ms.quizzes.microservices.question.service.QuestionService;
import pt.ulisboa.tecnico.socialsoftware.ms.causal.unityOfWork.CausalUnitOfWork;
import pt.ulisboa.tecnico.socialsoftware.ms.causal.unityOfWork.CausalUnitOfWorkService;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Profile("tcc")
@Service
public class CausalQuestionFunctionalities implements QuestionFunctionalitiesInterface {
    @Autowired
    private CausalUnitOfWorkService unitOfWorkService;
    @Autowired
    private QuestionService questionService;
    @Autowired
    private CourseService courseService;
    @Autowired
    private TopicService topicService;

    public QuestionDto findQuestionByAggregateId(Integer aggregateId) {
        CausalUnitOfWork unitOfWork = unitOfWorkService.createUnitOfWork(new Throwable().getStackTrace()[0].getMethodName());
        return questionService.getQuestionById(aggregateId, unitOfWork);
    }

    public List<QuestionDto> findQuestionsByCourseAggregateId(Integer courseAggregateId) {
        CausalUnitOfWork unitOfWork = unitOfWorkService.createUnitOfWork(new Throwable().getStackTrace()[0].getMethodName());
        return questionService.findQuestionsByCourseAggregateId(courseAggregateId, unitOfWork);
    }

    public QuestionDto createQuestion(Integer courseAggregateId, QuestionDto questionDto) {
        CausalUnitOfWork unitOfWork = unitOfWorkService.createUnitOfWork(new Throwable().getStackTrace()[0].getMethodName());
        QuestionCourse course = new QuestionCourse(courseService.getCourseById(courseAggregateId, unitOfWork));
        /*
            COURSE_SAME_TOPICS_COURSE
         */

        for (TopicDto topicDto : questionDto.getTopicDto()) {
            if (!topicDto.getCourseId().equals(courseAggregateId)) {
                throw new TutorException(ErrorMessage.QUESTION_TOPIC_INVALID_COURSE, topicDto.getAggregateId(), courseAggregateId);
            }
        }

        List<TopicDto> topics = questionDto.getTopicDto().stream()
                .map(topicDto -> topicService.getTopicById(topicDto.getAggregateId(), unitOfWork))
                .collect(Collectors.toList());

        QuestionDto questionDto1 = questionService.createQuestion(course, questionDto, topics, unitOfWork);

        unitOfWorkService.commit(unitOfWork);
        return questionDto1;
    }

    public void updateQuestion(QuestionDto questionDto) {
        CausalUnitOfWork unitOfWork = unitOfWorkService.createUnitOfWork(new Throwable().getStackTrace()[0].getMethodName());
        questionService.updateQuestion(questionDto, unitOfWork);

        unitOfWorkService.commit(unitOfWork);
    }

    public void removeQuestion(Integer questionAggregateId) {
        CausalUnitOfWork unitOfWork = unitOfWorkService.createUnitOfWork(new Throwable().getStackTrace()[0].getMethodName());
        questionService.removeQuestion(questionAggregateId, unitOfWork);

        unitOfWorkService.commit(unitOfWork);
    }


    public void updateQuestionTopics(Integer courseAggregateId, List<Integer> topicIds) {
        CausalUnitOfWork unitOfWork = unitOfWorkService.createUnitOfWork(new Throwable().getStackTrace()[0].getMethodName());
        Set<QuestionTopic> topics = topicIds.stream()
                        .map(id -> topicService.getTopicById(id, unitOfWork))
                        .map(QuestionTopic::new)
                        .collect(Collectors.toSet());

        questionService.updateQuestionTopics(courseAggregateId, topics, unitOfWork);
        unitOfWorkService.commit(unitOfWork);
    }

}
