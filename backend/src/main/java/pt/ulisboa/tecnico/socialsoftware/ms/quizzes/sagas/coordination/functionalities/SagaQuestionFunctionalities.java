package pt.ulisboa.tecnico.socialsoftware.ms.quizzes.sagas.coordination.functionalities;

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
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.unityOfWork.SagaUnitOfWork;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.unityOfWork.SagaUnitOfWorkService;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Profile("sagas")
@Service
public class SagaQuestionFunctionalities implements QuestionFunctionalitiesInterface{
    @Autowired
    private SagaUnitOfWorkService unitOfWorkService;
    @Autowired
    private QuestionService questionService;
    @Autowired
    private CourseService courseService;
    @Autowired
    private TopicService topicService;

    public QuestionDto findQuestionByAggregateId(Integer aggregateId) {
        SagaUnitOfWork unitOfWork = unitOfWorkService.createUnitOfWork(new Throwable().getStackTrace()[0].getMethodName());
        return questionService.getQuestionById(aggregateId, unitOfWork);
    }

    public List<QuestionDto> findQuestionsByCourseAggregateId(Integer courseAggregateId) {
        SagaUnitOfWork unitOfWork = unitOfWorkService.createUnitOfWork(new Throwable().getStackTrace()[0].getMethodName());
        return questionService.findQuestionsByCourseAggregateId(courseAggregateId, unitOfWork);
    }

    public QuestionDto createQuestion(Integer courseAggregateId, QuestionDto questionDto) throws Exception {
        SagaUnitOfWork unitOfWork = unitOfWorkService.createUnitOfWork(new Throwable().getStackTrace()[0].getMethodName());
        try {
        
            // Validate Question Topics
            for (TopicDto topicDto : questionDto.getTopicDto()) {
                if (!topicDto.getCourseId().equals(courseAggregateId)) {
                    throw new TutorException(ErrorMessage.QUESTION_TOPIC_INVALID_COURSE, topicDto.getAggregateId(), courseAggregateId);
                }
            }

            // Retrieve Course
            QuestionCourse course = new QuestionCourse(courseService.getCourseById(courseAggregateId, unitOfWork));

            // Retrieve Topics
            List<TopicDto> topics = questionDto.getTopicDto().stream()
                    .map(topicDto -> topicService.getTopicById(topicDto.getAggregateId(), unitOfWork))
                    .collect(Collectors.toList());

            // Create Question
            QuestionDto createdQuestion = questionService.createQuestion(course, questionDto, topics, unitOfWork);
            
            //TODO
            // unitOfWork.registerCompensation(() -> questionService.removeQuestion(createdQuestion.getAggregateId(), unitOfWork));
        
            unitOfWorkService.commit(unitOfWork);
            return createdQuestion;
        } catch (Exception ex) {
            unitOfWorkService.compensate(unitOfWork);
            throw new Exception("Error creating question", ex);
        }
    }

    public void updateQuestion(QuestionDto questionDto) throws Exception {
        SagaUnitOfWork unitOfWork = unitOfWorkService.createUnitOfWork(new Throwable().getStackTrace()[0].getMethodName());
        try {
            QuestionDto originalQuestion = questionService.getQuestionById(questionDto.getAggregateId(), unitOfWork);
    
            questionService.updateQuestion(questionDto, unitOfWork);
    
            //TODO
            //unitOfWork.registerCompensation(() -> questionService.updateQuestion(originalQuestion, unitOfWork));
    
            unitOfWorkService.commit(unitOfWork);
        } catch (Exception ex) {
            unitOfWorkService.compensate(unitOfWork);
            throw new Exception("Error updating question", ex);
        }
    }

    public void removeQuestion(Integer questionAggregateId) throws Exception {
        SagaUnitOfWork unitOfWork = unitOfWorkService.createUnitOfWork(new Throwable().getStackTrace()[0].getMethodName());
        try {
            //TODO register compensate with saved question
            //unitOfWork.registerCompensation(()

            questionService.removeQuestion(questionAggregateId, unitOfWork);
    
            unitOfWorkService.commit(unitOfWork);
        } catch (Exception ex) {
            unitOfWorkService.compensate(unitOfWork);
            throw new Exception("Error removing question", ex);
        }
    }


    public void updateQuestionTopics(Integer courseAggregateId, List<Integer> topicIds) throws Exception {
        SagaUnitOfWork unitOfWork = unitOfWorkService.createUnitOfWork(new Throwable().getStackTrace()[0].getMethodName());
        try {
            Set<QuestionTopic> topics = topicIds.stream()
                            .map(id -> topicService.getTopicById(id, unitOfWork))
                            .map(QuestionTopic::new)
                            .collect(Collectors.toSet());

            questionService.updateQuestionTopics(courseAggregateId, topics, unitOfWork);
            // TODO
            // unitOfWork.registerCompensation(() -> questionService.updateQuestionTopics(questionAggregateId, originalTopics, unitOfWork));

            unitOfWorkService.commit(unitOfWork);
        } catch (Exception ex) {
            unitOfWorkService.compensate(unitOfWork);
            throw new Exception("Error updating question topics", ex);
        }
    }

}
