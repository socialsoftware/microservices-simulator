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
import pt.ulisboa.tecnico.socialsoftware.ms.quizzes.sagas.coordination.functionalitiesWorkflows.CreateQuestionFunctionality;
import pt.ulisboa.tecnico.socialsoftware.ms.quizzes.sagas.coordination.functionalitiesWorkflows.FindQuestionByAggregateIdFunctionality;
import pt.ulisboa.tecnico.socialsoftware.ms.quizzes.sagas.coordination.functionalitiesWorkflows.FindQuestionsByCourseFunctionality;
import pt.ulisboa.tecnico.socialsoftware.ms.quizzes.sagas.coordination.functionalitiesWorkflows.RemoveQuestionFunctionality;
import pt.ulisboa.tecnico.socialsoftware.ms.quizzes.sagas.coordination.functionalitiesWorkflows.UpdateQuestionFunctionality;
import pt.ulisboa.tecnico.socialsoftware.ms.quizzes.sagas.coordination.functionalitiesWorkflows.UpdateQuestionTopicsFunctionality;
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

        FindQuestionByAggregateIdFunctionality functionality = new FindQuestionByAggregateIdFunctionality(questionService, unitOfWorkService, aggregateId, unitOfWork);

        functionality.executeWorkflow(unitOfWork);
        return functionality.getQuestionDto();
    }

    public List<QuestionDto> findQuestionsByCourseAggregateId(Integer courseAggregateId) {
        String functionalityName = new Throwable().getStackTrace()[0].getMethodName();
        SagaUnitOfWork unitOfWork = unitOfWorkService.createUnitOfWork(functionalityName);
    
        FindQuestionsByCourseFunctionality functionality = new FindQuestionsByCourseFunctionality(questionService, unitOfWorkService, courseAggregateId, unitOfWork);
        
        functionality.executeWorkflow(unitOfWork);
        return functionality.getQuestions();
    }

    public QuestionDto createQuestion(Integer courseAggregateId, QuestionDto questionDto) throws Exception {
        String functionalityName = new Throwable().getStackTrace()[0].getMethodName();
        SagaUnitOfWork unitOfWork = unitOfWorkService.createUnitOfWork(functionalityName);
    
        CreateQuestionFunctionality functionality = new CreateQuestionFunctionality(questionService, topicService, courseService, unitOfWorkService, courseAggregateId, questionDto, unitOfWork);
        
        functionality.executeWorkflow(unitOfWork);
        return functionality.getCreatedQuestion();
    }

    public void updateQuestion(QuestionDto questionDto) throws Exception {
        String functionalityName = new Throwable().getStackTrace()[0].getMethodName();
        SagaUnitOfWork unitOfWork = unitOfWorkService.createUnitOfWork(functionalityName);
    
        UpdateQuestionFunctionality functionality = new UpdateQuestionFunctionality(questionService, unitOfWorkService, questionFactory, questionDto, unitOfWork);
        
        functionality.executeWorkflow(unitOfWork);
    }

    public void removeQuestion(Integer questionAggregateId) throws Exception {
        String functionalityName = new Throwable().getStackTrace()[0].getMethodName();
        SagaUnitOfWork unitOfWork = unitOfWorkService.createUnitOfWork(functionalityName);
    
        RemoveQuestionFunctionality functionality = new RemoveQuestionFunctionality(questionService, unitOfWorkService, questionAggregateId, unitOfWork);
        
        functionality.executeWorkflow(unitOfWork);
    }


    public void updateQuestionTopics(Integer courseAggregateId, List<Integer> topicIds) throws Exception {
        String functionalityName = new Throwable().getStackTrace()[0].getMethodName();
        SagaUnitOfWork unitOfWork = unitOfWorkService.createUnitOfWork(functionalityName);
    
        UpdateQuestionTopicsFunctionality functionality = new UpdateQuestionTopicsFunctionality(questionService, topicService, questionFactory, unitOfWorkService, courseAggregateId, topicIds, unitOfWork);
        
        functionality.executeWorkflow(unitOfWork);
    }

}
