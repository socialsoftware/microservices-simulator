package pt.ulisboa.tecnico.socialsoftware.ms.quizzes.coordination.functionalities;

import java.util.Arrays;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;
import pt.ulisboa.tecnico.socialsoftware.ms.causal.unityOfWork.CausalUnitOfWork;
import pt.ulisboa.tecnico.socialsoftware.ms.causal.unityOfWork.CausalUnitOfWorkService;
import pt.ulisboa.tecnico.socialsoftware.ms.quizzes.causal.coordination.workflows.CreateQuizFunctionalityTCC;
import pt.ulisboa.tecnico.socialsoftware.ms.quizzes.causal.coordination.workflows.FindQuizFunctionalityTCC;
import pt.ulisboa.tecnico.socialsoftware.ms.quizzes.causal.coordination.workflows.GetAvailableQuizzesFunctionalityTCC;
import pt.ulisboa.tecnico.socialsoftware.ms.quizzes.causal.coordination.workflows.UpdateQuizFunctionalityTCC;
import pt.ulisboa.tecnico.socialsoftware.ms.quizzes.microservices.execution.service.CourseExecutionService;
import pt.ulisboa.tecnico.socialsoftware.ms.quizzes.microservices.question.service.QuestionService;
import pt.ulisboa.tecnico.socialsoftware.ms.quizzes.microservices.quiz.aggregate.QuizDto;
import pt.ulisboa.tecnico.socialsoftware.ms.quizzes.microservices.quiz.aggregate.QuizFactory;
import pt.ulisboa.tecnico.socialsoftware.ms.quizzes.microservices.quiz.service.QuizService;
import pt.ulisboa.tecnico.socialsoftware.ms.quizzes.sagas.coordination.workflows.CreateQuizFunctionalitySagas;
import pt.ulisboa.tecnico.socialsoftware.ms.quizzes.sagas.coordination.workflows.FindQuizFunctionalitySagas;
import pt.ulisboa.tecnico.socialsoftware.ms.quizzes.sagas.coordination.workflows.GetAvailableQuizzesFunctionalitySagas;
import pt.ulisboa.tecnico.socialsoftware.ms.quizzes.sagas.coordination.workflows.UpdateQuizFunctionalitySagas;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.unityOfWork.SagaUnitOfWork;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.unityOfWork.SagaUnitOfWorkService;

@Service
public class QuizFunctionalities {
    @Autowired(required = false)
    private SagaUnitOfWorkService sagaUnitOfWorkService;
    @Autowired(required = false)
    private CausalUnitOfWorkService causalUnitOfWorkService;
    @Autowired
    private QuizService quizService;
    @Autowired
    private CourseExecutionService courseExecutionService;
    @Autowired
    private QuestionService questionService;
    @Autowired
    private QuizFactory quizFactory;

    @Autowired
    private Environment env;

    private String workflowType;

    @PostConstruct
    public void init() {
        // Determine the workflow type based on active profiles
        String[] activeProfiles = env.getActiveProfiles();
        if (Arrays.asList(activeProfiles).contains("sagas")) {
            workflowType = "sagas";
        } else if (Arrays.asList(activeProfiles).contains("tcc")) {
            workflowType = "tcc";
        } else {
            workflowType = "unknown"; // Default or fallback value
        }
    }

    public QuizDto createQuiz(Integer courseExecutionId, QuizDto quizDto) throws Exception {
        String functionalityName = new Throwable().getStackTrace()[0].getMethodName();

        if ("sagas".equals(workflowType)) {
            SagaUnitOfWork unitOfWork = sagaUnitOfWorkService.createUnitOfWork(functionalityName);
            CreateQuizFunctionalitySagas functionality = new CreateQuizFunctionalitySagas(
                    courseExecutionService, quizService, questionService, sagaUnitOfWorkService, courseExecutionId, quizDto, unitOfWork);
            functionality.executeWorkflow(unitOfWork);
            return functionality.getCreatedQuizDto();
        } else {
            CausalUnitOfWork unitOfWork = causalUnitOfWorkService.createUnitOfWork(functionalityName);
            CreateQuizFunctionalityTCC functionality = new CreateQuizFunctionalityTCC(
                    courseExecutionService, quizService, questionService, causalUnitOfWorkService, courseExecutionId, quizDto, unitOfWork);
            functionality.executeWorkflow(unitOfWork);
            return functionality.getCreatedQuizDto();
        }
    }

    public QuizDto findQuiz(Integer quizAggregateId) {
        String functionalityName = new Throwable().getStackTrace()[0].getMethodName();

        if ("sagas".equals(workflowType)) {
            SagaUnitOfWork unitOfWork = sagaUnitOfWorkService.createUnitOfWork(functionalityName);
            FindQuizFunctionalitySagas functionality = new FindQuizFunctionalitySagas(
                    quizService, sagaUnitOfWorkService, quizAggregateId, unitOfWork);
            functionality.executeWorkflow(unitOfWork);
            return functionality.getQuizDto();
        } else {
            CausalUnitOfWork unitOfWork = causalUnitOfWorkService.createUnitOfWork(functionalityName);
            FindQuizFunctionalityTCC functionality = new FindQuizFunctionalityTCC(
                    quizService, causalUnitOfWorkService, quizAggregateId, unitOfWork);
            functionality.executeWorkflow(unitOfWork);
            return functionality.getQuizDto();
        }
    }

    public List<QuizDto> getAvailableQuizzes(Integer userAggregateId, Integer courseExecutionAggregateId) {
        String functionalityName = new Throwable().getStackTrace()[0].getMethodName();

        if ("sagas".equals(workflowType)) {
            SagaUnitOfWork unitOfWork = sagaUnitOfWorkService.createUnitOfWork(functionalityName);
            GetAvailableQuizzesFunctionalitySagas functionality = new GetAvailableQuizzesFunctionalitySagas(
                    quizService, sagaUnitOfWorkService, courseExecutionAggregateId, courseExecutionAggregateId, unitOfWork);
            functionality.executeWorkflow(unitOfWork);
            return functionality.getAvailableQuizzes();
        } else {
            CausalUnitOfWork unitOfWork = causalUnitOfWorkService.createUnitOfWork(functionalityName);
            GetAvailableQuizzesFunctionalityTCC functionality = new GetAvailableQuizzesFunctionalityTCC(
                    quizService, causalUnitOfWorkService, courseExecutionAggregateId, courseExecutionAggregateId, unitOfWork);
            functionality.executeWorkflow(unitOfWork);
            return functionality.getAvailableQuizzes();
        }
    }

    public QuizDto updateQuiz(QuizDto quizDto) throws Exception {
        String functionalityName = new Throwable().getStackTrace()[0].getMethodName();

        if ("sagas".equals(workflowType)) {
            SagaUnitOfWork unitOfWork = sagaUnitOfWorkService.createUnitOfWork(functionalityName);
            UpdateQuizFunctionalitySagas functionality = new UpdateQuizFunctionalitySagas(
                    quizService, sagaUnitOfWorkService, quizFactory, quizDto, unitOfWork);
            functionality.executeWorkflow(unitOfWork);
            return functionality.getUpdatedQuizDto();
        } else {
            CausalUnitOfWork unitOfWork = causalUnitOfWorkService.createUnitOfWork(functionalityName);
            UpdateQuizFunctionalityTCC functionality = new UpdateQuizFunctionalityTCC(
                    quizService, causalUnitOfWorkService, quizFactory, quizDto, unitOfWork);
            functionality.executeWorkflow(unitOfWork);
            return functionality.getUpdatedQuizDto();
        }
    }

}
