package pt.ulisboa.tecnico.socialsoftware.ms.quizzes.sagas.coordination.functionalities;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

import pt.ulisboa.tecnico.socialsoftware.ms.quizzes.microservices.course.service.CourseService;
import pt.ulisboa.tecnico.socialsoftware.ms.quizzes.microservices.question.aggregate.QuestionDto;
import pt.ulisboa.tecnico.socialsoftware.ms.quizzes.microservices.question.aggregate.QuestionFactory;
import pt.ulisboa.tecnico.socialsoftware.ms.quizzes.microservices.question.service.QuestionFunctionalitiesInterface;
import pt.ulisboa.tecnico.socialsoftware.ms.quizzes.microservices.question.service.QuestionService;
import pt.ulisboa.tecnico.socialsoftware.ms.quizzes.microservices.topic.service.TopicService;
import pt.ulisboa.tecnico.socialsoftware.ms.quizzes.sagas.coordination.data.CreateQuestionData;
import pt.ulisboa.tecnico.socialsoftware.ms.quizzes.sagas.coordination.data.FindQuestionByAggregateIdData;
import pt.ulisboa.tecnico.socialsoftware.ms.quizzes.sagas.coordination.data.FindQuestionsByCourseData;
import pt.ulisboa.tecnico.socialsoftware.ms.quizzes.sagas.coordination.data.RemoveQuestionData;
import pt.ulisboa.tecnico.socialsoftware.ms.quizzes.sagas.coordination.data.UpdateQuestionData;
import pt.ulisboa.tecnico.socialsoftware.ms.quizzes.sagas.coordination.data.UpdateQuestionTopicsData;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.unityOfWork.SagaUnitOfWork;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.unityOfWork.SagaUnitOfWorkService;

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
    @Autowired
    private QuestionFactory questionFactory;

    public QuestionDto findQuestionByAggregateId(Integer aggregateId) {
        String functionalityName = new Throwable().getStackTrace()[0].getMethodName();
        SagaUnitOfWork unitOfWork = unitOfWorkService.createUnitOfWork(functionalityName);

        FindQuestionByAggregateIdData data = new FindQuestionByAggregateIdData(questionService, unitOfWorkService, aggregateId, unitOfWork);

        data.executeWorkflow(unitOfWork);
        return data.getQuestionDto();
    }

    public List<QuestionDto> findQuestionsByCourseAggregateId(Integer courseAggregateId) {
        String functionalityName = new Throwable().getStackTrace()[0].getMethodName();
        SagaUnitOfWork unitOfWork = unitOfWorkService.createUnitOfWork(functionalityName);
    
        FindQuestionsByCourseData data = new FindQuestionsByCourseData(questionService, unitOfWorkService, courseAggregateId, unitOfWork);
        
        data.executeWorkflow(unitOfWork);
        return data.getQuestions();
    }

    public QuestionDto createQuestion(Integer courseAggregateId, QuestionDto questionDto) throws Exception {
        String functionalityName = new Throwable().getStackTrace()[0].getMethodName();
        SagaUnitOfWork unitOfWork = unitOfWorkService.createUnitOfWork(functionalityName);
    
        CreateQuestionData data = new CreateQuestionData(questionService, topicService, courseService, unitOfWorkService, courseAggregateId, questionDto, unitOfWork);
        
        data.executeWorkflow(unitOfWork);
        return data.getCreatedQuestion();
    }

    public void updateQuestion(QuestionDto questionDto) throws Exception {
        String functionalityName = new Throwable().getStackTrace()[0].getMethodName();
        SagaUnitOfWork unitOfWork = unitOfWorkService.createUnitOfWork(functionalityName);
    
        UpdateQuestionData data = new UpdateQuestionData(questionService, unitOfWorkService, questionFactory, questionDto, unitOfWork);
        
        data.executeWorkflow(unitOfWork);
    }

    public void removeQuestion(Integer questionAggregateId) throws Exception {
        String functionalityName = new Throwable().getStackTrace()[0].getMethodName();
        SagaUnitOfWork unitOfWork = unitOfWorkService.createUnitOfWork(functionalityName);
    
        RemoveQuestionData data = new RemoveQuestionData(questionService, unitOfWorkService, questionAggregateId, unitOfWork);
        
        data.executeWorkflow(unitOfWork);
    }


    public void updateQuestionTopics(Integer courseAggregateId, List<Integer> topicIds) throws Exception {
        String functionalityName = new Throwable().getStackTrace()[0].getMethodName();
        SagaUnitOfWork unitOfWork = unitOfWorkService.createUnitOfWork(functionalityName);
    
        UpdateQuestionTopicsData data = new UpdateQuestionTopicsData(questionService, topicService, questionFactory, unitOfWorkService, courseAggregateId, topicIds, unitOfWork);
        
        data.executeWorkflow(unitOfWork);
    }

}
