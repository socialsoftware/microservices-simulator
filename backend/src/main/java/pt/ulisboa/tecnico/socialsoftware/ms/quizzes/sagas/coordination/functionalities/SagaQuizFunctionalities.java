package pt.ulisboa.tecnico.socialsoftware.ms.quizzes.sagas.coordination.functionalities;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

import pt.ulisboa.tecnico.socialsoftware.ms.quizzes.microservices.execution.service.CourseExecutionService;
import pt.ulisboa.tecnico.socialsoftware.ms.quizzes.microservices.question.service.QuestionService;
import pt.ulisboa.tecnico.socialsoftware.ms.quizzes.microservices.quiz.aggregate.QuizDto;
import pt.ulisboa.tecnico.socialsoftware.ms.quizzes.microservices.quiz.aggregate.QuizFactory;
import pt.ulisboa.tecnico.socialsoftware.ms.quizzes.microservices.quiz.service.QuizFunctionalitiesInterface;
import pt.ulisboa.tecnico.socialsoftware.ms.quizzes.microservices.quiz.service.QuizService;
import pt.ulisboa.tecnico.socialsoftware.ms.quizzes.sagas.coordination.functionalitiesWorkflows.CreateQuizFunctionality;
import pt.ulisboa.tecnico.socialsoftware.ms.quizzes.sagas.coordination.functionalitiesWorkflows.FindQuizFunctionality;
import pt.ulisboa.tecnico.socialsoftware.ms.quizzes.sagas.coordination.functionalitiesWorkflows.GetAvailableQuizzesFunctionality;
import pt.ulisboa.tecnico.socialsoftware.ms.quizzes.sagas.coordination.functionalitiesWorkflows.UpdateQuizFunctionality;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.unityOfWork.SagaUnitOfWork;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.unityOfWork.SagaUnitOfWorkService;

@Profile("sagas")
@Service
public class SagaQuizFunctionalities implements QuizFunctionalitiesInterface{
    @Autowired
    private SagaUnitOfWorkService unitOfWorkService;
    @Autowired
    private QuizService quizService;
    @Autowired
    private CourseExecutionService courseExecutionService;
    @Autowired
    private QuestionService questionService;
    @Autowired
    private QuizFactory quizFactory;

    public QuizDto createQuiz(Integer courseExecutionId, QuizDto quizDto) throws Exception {
        String functionalityName = new Throwable().getStackTrace()[0].getMethodName();
        SagaUnitOfWork unitOfWork = unitOfWorkService.createUnitOfWork(functionalityName);

        CreateQuizFunctionality functionality = new CreateQuizFunctionality(courseExecutionService, quizService, questionService, unitOfWorkService, courseExecutionId, quizDto, unitOfWork);
        
        functionality.executeWorkflow(unitOfWork);
        return functionality.getCreatedQuizDto();
    }

    public QuizDto findQuiz(Integer quizAggregateId) {
        String functionalityName = new Throwable().getStackTrace()[0].getMethodName();
        SagaUnitOfWork unitOfWork = unitOfWorkService.createUnitOfWork(functionalityName);
    
        FindQuizFunctionality functionality = new FindQuizFunctionality(quizService, unitOfWorkService, quizAggregateId, unitOfWork);
        
        functionality.executeWorkflow(unitOfWork);
        return functionality.getQuizDto();
    }

    public List<QuizDto> getAvailableQuizzes(Integer userAggregateId, Integer courseExecutionAggregateId) {
        String functionalityName = new Throwable().getStackTrace()[0].getMethodName();
        SagaUnitOfWork unitOfWork = unitOfWorkService.createUnitOfWork(functionalityName);
    
        GetAvailableQuizzesFunctionality functionality = new GetAvailableQuizzesFunctionality(quizService, unitOfWorkService, courseExecutionAggregateId, courseExecutionAggregateId, unitOfWork);
        
        functionality.executeWorkflow(unitOfWork);
        return functionality.getAvailableQuizzes();
    }

    public QuizDto updateQuiz(QuizDto quizDto) throws Exception {
        String functionalityName = new Throwable().getStackTrace()[0].getMethodName();
        SagaUnitOfWork unitOfWork = unitOfWorkService.createUnitOfWork(functionalityName);
    
        UpdateQuizFunctionality functionality = new UpdateQuizFunctionality(quizService, unitOfWorkService, quizFactory, quizDto, unitOfWork);
        
        functionality.executeWorkflow(unitOfWork);
        return functionality.getUpdatedQuizDto();
    }

}
