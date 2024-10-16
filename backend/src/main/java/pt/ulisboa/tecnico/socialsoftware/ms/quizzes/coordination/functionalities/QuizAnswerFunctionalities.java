package pt.ulisboa.tecnico.socialsoftware.ms.quizzes.coordination.functionalities;

import java.util.Arrays;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;
import pt.ulisboa.tecnico.socialsoftware.ms.causal.unityOfWork.CausalUnitOfWork;
import pt.ulisboa.tecnico.socialsoftware.ms.causal.unityOfWork.CausalUnitOfWorkService;
import pt.ulisboa.tecnico.socialsoftware.ms.quizzes.causal.coordination.AnswerQuestionFunctionalityTCC;
import pt.ulisboa.tecnico.socialsoftware.ms.quizzes.causal.coordination.ConcludeQuizFunctionalityTCC;
import pt.ulisboa.tecnico.socialsoftware.ms.quizzes.causal.coordination.StartQuizFunctionalityTCC;
import pt.ulisboa.tecnico.socialsoftware.ms.quizzes.microservices.answer.aggregate.QuestionAnswerDto;
import pt.ulisboa.tecnico.socialsoftware.ms.quizzes.microservices.answer.aggregate.QuizAnswerFactory;
import pt.ulisboa.tecnico.socialsoftware.ms.quizzes.microservices.answer.service.QuizAnswerService;
import pt.ulisboa.tecnico.socialsoftware.ms.quizzes.microservices.question.service.QuestionService;
import pt.ulisboa.tecnico.socialsoftware.ms.quizzes.microservices.quiz.service.QuizService;
import pt.ulisboa.tecnico.socialsoftware.ms.quizzes.sagas.coordination.workflows.AnswerQuestionFunctionalitySagas;
import pt.ulisboa.tecnico.socialsoftware.ms.quizzes.sagas.coordination.workflows.ConcludeQuizFunctionalitySagas;
import pt.ulisboa.tecnico.socialsoftware.ms.quizzes.sagas.coordination.workflows.StartQuizFunctionalitySagas;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.unityOfWork.SagaUnitOfWork;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.unityOfWork.SagaUnitOfWorkService;

@Service
public class QuizAnswerFunctionalities {
    @Autowired
    private QuizAnswerService quizAnswerService;
    @Autowired
    private QuestionService questionService;
    @Autowired(required = false)
    private SagaUnitOfWorkService sagaUnitOfWorkService;
    @Autowired(required = false)
    private CausalUnitOfWorkService causalUnitOfWorkService;
    @Autowired
    private QuizAnswerFactory quizAnswerFactory;
    @Autowired
    private QuizService quizService;

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

    public void answerQuestion(Integer quizAggregateId, Integer userAggregateId, QuestionAnswerDto userQuestionAnswerDto) throws Exception {
        String functionalityName = new Throwable().getStackTrace()[0].getMethodName();

        if ("sagas".equals(workflowType)) {
            SagaUnitOfWork unitOfWork = sagaUnitOfWorkService.createUnitOfWork(functionalityName);
            AnswerQuestionFunctionalitySagas functionality = new AnswerQuestionFunctionalitySagas(
                    quizAnswerService, questionService, sagaUnitOfWorkService, quizAnswerFactory, quizAggregateId, userAggregateId, userQuestionAnswerDto, unitOfWork);
            functionality.executeWorkflow(unitOfWork);
        } else {
            CausalUnitOfWork unitOfWork = causalUnitOfWorkService.createUnitOfWork(functionalityName);
            AnswerQuestionFunctionalityTCC functionality = new AnswerQuestionFunctionalityTCC(
                    quizAnswerService, questionService, causalUnitOfWorkService, quizAnswerFactory, quizAggregateId, userAggregateId, userQuestionAnswerDto, unitOfWork);
            functionality.executeWorkflow(unitOfWork);
        }
    }

    public void startQuiz(Integer quizAggregateId, Integer courseExecutionAggregateId, Integer userAggregateId) throws Exception {
        String functionalityName = new Throwable().getStackTrace()[0].getMethodName();

        if ("sagas".equals(workflowType)) {
            SagaUnitOfWork unitOfWork = sagaUnitOfWorkService.createUnitOfWork(functionalityName);
            StartQuizFunctionalitySagas functionality = new StartQuizFunctionalitySagas(
                    quizAnswerService, quizService, sagaUnitOfWorkService, quizAggregateId, courseExecutionAggregateId, userAggregateId, unitOfWork);
            functionality.executeWorkflow(unitOfWork);
        } else {
            CausalUnitOfWork unitOfWork = causalUnitOfWorkService.createUnitOfWork(functionalityName);
            StartQuizFunctionalityTCC functionality = new StartQuizFunctionalityTCC(
                    quizAnswerService, quizService, causalUnitOfWorkService, quizAggregateId, courseExecutionAggregateId, userAggregateId, unitOfWork);
            functionality.executeWorkflow(unitOfWork);
        }
    }

    public void concludeQuiz(Integer quizAggregateId, Integer userAggregateId) throws Exception {
        String functionalityName = new Throwable().getStackTrace()[0].getMethodName();

        if ("sagas".equals(workflowType)) {
            SagaUnitOfWork unitOfWork = sagaUnitOfWorkService.createUnitOfWork(functionalityName);
            ConcludeQuizFunctionalitySagas functionality = new ConcludeQuizFunctionalitySagas(
                    quizAnswerService, sagaUnitOfWorkService, quizAggregateId, userAggregateId, unitOfWork);
            functionality.executeWorkflow(unitOfWork);
        } else {
            CausalUnitOfWork unitOfWork = causalUnitOfWorkService.createUnitOfWork(functionalityName);
            ConcludeQuizFunctionalityTCC functionality = new ConcludeQuizFunctionalityTCC(
                    quizAnswerService, causalUnitOfWorkService, quizAggregateId, userAggregateId, unitOfWork);
            functionality.executeWorkflow(unitOfWork);
        }
    }

}