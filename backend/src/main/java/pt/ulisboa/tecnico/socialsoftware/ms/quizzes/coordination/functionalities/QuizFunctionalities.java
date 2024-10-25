package pt.ulisboa.tecnico.socialsoftware.ms.quizzes.coordination.functionalities;

import java.util.Arrays;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;
import pt.ulisboa.tecnico.socialsoftware.ms.MicroservicesSimulator;
import pt.ulisboa.tecnico.socialsoftware.ms.causal.unityOfWork.CausalUnitOfWork;
import pt.ulisboa.tecnico.socialsoftware.ms.causal.unityOfWork.CausalUnitOfWorkService;
import pt.ulisboa.tecnico.socialsoftware.ms.quizzes.causal.coordination.quiz.CreateQuizFunctionalityTCC;
import pt.ulisboa.tecnico.socialsoftware.ms.quizzes.causal.coordination.quiz.FindQuizFunctionalityTCC;
import pt.ulisboa.tecnico.socialsoftware.ms.quizzes.causal.coordination.quiz.GetAvailableQuizzesFunctionalityTCC;
import pt.ulisboa.tecnico.socialsoftware.ms.quizzes.causal.coordination.quiz.UpdateQuizFunctionalityTCC;
import pt.ulisboa.tecnico.socialsoftware.ms.quizzes.microservices.exception.TutorException;
import pt.ulisboa.tecnico.socialsoftware.ms.quizzes.microservices.execution.service.CourseExecutionService;
import pt.ulisboa.tecnico.socialsoftware.ms.quizzes.microservices.question.service.QuestionService;
import pt.ulisboa.tecnico.socialsoftware.ms.quizzes.microservices.quiz.aggregate.QuizDto;
import pt.ulisboa.tecnico.socialsoftware.ms.quizzes.microservices.quiz.aggregate.QuizFactory;
import pt.ulisboa.tecnico.socialsoftware.ms.quizzes.microservices.quiz.service.QuizService;
import pt.ulisboa.tecnico.socialsoftware.ms.quizzes.sagas.coordination.quiz.CreateQuizFunctionalitySagas;
import pt.ulisboa.tecnico.socialsoftware.ms.quizzes.sagas.coordination.quiz.FindQuizFunctionalitySagas;
import pt.ulisboa.tecnico.socialsoftware.ms.quizzes.sagas.coordination.quiz.GetAvailableQuizzesFunctionalitySagas;
import pt.ulisboa.tecnico.socialsoftware.ms.quizzes.sagas.coordination.quiz.UpdateQuizFunctionalitySagas;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.unityOfWork.SagaUnitOfWork;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.unityOfWork.SagaUnitOfWorkService;

import static pt.ulisboa.tecnico.socialsoftware.ms.MicroservicesSimulator.TransactionalModel.SAGAS;
import static pt.ulisboa.tecnico.socialsoftware.ms.MicroservicesSimulator.TransactionalModel.TCC;
import static pt.ulisboa.tecnico.socialsoftware.ms.quizzes.microservices.exception.ErrorMessage.UNDEFINED_TRANSACTIONAL_MODEL;

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

    private MicroservicesSimulator.TransactionalModel workflowType;

    @PostConstruct
    public void init() {
        String[] activeProfiles = env.getActiveProfiles();
        if (Arrays.asList(activeProfiles).contains(SAGAS.getValue())) {
            workflowType = SAGAS;
        } else if (Arrays.asList(activeProfiles).contains(TCC.getValue())) {
            workflowType = TCC;
        } else {
            throw new TutorException(UNDEFINED_TRANSACTIONAL_MODEL);
        }
    }

    public QuizDto createQuiz(Integer courseExecutionId, QuizDto quizDto) throws Exception {
        String functionalityName = new Throwable().getStackTrace()[0].getMethodName();

        switch (workflowType) {
            case SAGAS:
                SagaUnitOfWork sagaUnitOfWork = sagaUnitOfWorkService.createUnitOfWork(functionalityName);
                CreateQuizFunctionalitySagas createQuizFunctionalitySagas = new CreateQuizFunctionalitySagas(
                        courseExecutionService, quizService, questionService, sagaUnitOfWorkService, courseExecutionId, quizDto, sagaUnitOfWork);
                createQuizFunctionalitySagas.executeWorkflow(sagaUnitOfWork);
                return createQuizFunctionalitySagas.getCreatedQuizDto();
            case TCC:
                CausalUnitOfWork causalUnitOfWork = causalUnitOfWorkService.createUnitOfWork(functionalityName);
                CreateQuizFunctionalityTCC createQuizFunctionalityTCC = new CreateQuizFunctionalityTCC(
                        courseExecutionService, quizService, questionService, causalUnitOfWorkService, courseExecutionId, quizDto, causalUnitOfWork);
                createQuizFunctionalityTCC.executeWorkflow(causalUnitOfWork);
                return createQuizFunctionalityTCC.getCreatedQuizDto();
            default: throw new TutorException(UNDEFINED_TRANSACTIONAL_MODEL);
        }
    }

    public QuizDto findQuiz(Integer quizAggregateId) {
        String functionalityName = new Throwable().getStackTrace()[0].getMethodName();

        switch (workflowType) {
            case SAGAS:
                SagaUnitOfWork sagaUnitOfWork = sagaUnitOfWorkService.createUnitOfWork(functionalityName);
                FindQuizFunctionalitySagas findQuizFunctionalitySagas = new FindQuizFunctionalitySagas(
                        quizService, sagaUnitOfWorkService, quizAggregateId, sagaUnitOfWork);
                findQuizFunctionalitySagas.executeWorkflow(sagaUnitOfWork);
                return findQuizFunctionalitySagas.getQuizDto();
            case TCC:
                CausalUnitOfWork causalUnitOfWork = causalUnitOfWorkService.createUnitOfWork(functionalityName);
                FindQuizFunctionalityTCC findQuizFunctionalityTCC = new FindQuizFunctionalityTCC(
                        quizService, causalUnitOfWorkService, quizAggregateId, causalUnitOfWork);
                findQuizFunctionalityTCC.executeWorkflow(causalUnitOfWork);
                return findQuizFunctionalityTCC.getQuizDto();
            default: throw new TutorException(UNDEFINED_TRANSACTIONAL_MODEL);
        }
    }

    public List<QuizDto> getAvailableQuizzes(Integer userAggregateId, Integer courseExecutionAggregateId) {
        String functionalityName = new Throwable().getStackTrace()[0].getMethodName();

        switch (workflowType) {
            case SAGAS:
                SagaUnitOfWork sagaUnitOfWork = sagaUnitOfWorkService.createUnitOfWork(functionalityName);
                GetAvailableQuizzesFunctionalitySagas getAvailableQuizzesFunctionalitySagas = new GetAvailableQuizzesFunctionalitySagas(
                        quizService, sagaUnitOfWorkService, courseExecutionAggregateId, courseExecutionAggregateId, sagaUnitOfWork);
                getAvailableQuizzesFunctionalitySagas.executeWorkflow(sagaUnitOfWork);
                return getAvailableQuizzesFunctionalitySagas.getAvailableQuizzes();
            case TCC:
                CausalUnitOfWork causalUnitOfWork = causalUnitOfWorkService.createUnitOfWork(functionalityName);
                GetAvailableQuizzesFunctionalityTCC getAvailableQuizzesFunctionalityTCC = new GetAvailableQuizzesFunctionalityTCC(
                        quizService, causalUnitOfWorkService, courseExecutionAggregateId, courseExecutionAggregateId, causalUnitOfWork);
                getAvailableQuizzesFunctionalityTCC.executeWorkflow(causalUnitOfWork);
                return getAvailableQuizzesFunctionalityTCC.getAvailableQuizzes();
            default: throw new TutorException(UNDEFINED_TRANSACTIONAL_MODEL);
        }
    }

    public QuizDto updateQuiz(QuizDto quizDto) throws Exception {
        String functionalityName = new Throwable().getStackTrace()[0].getMethodName();

        switch (workflowType) {
            case SAGAS:
                SagaUnitOfWork sagaUnitOfWork = sagaUnitOfWorkService.createUnitOfWork(functionalityName);
                UpdateQuizFunctionalitySagas updateQuizFunctionalitySagas = new UpdateQuizFunctionalitySagas(
                        quizService, sagaUnitOfWorkService, quizFactory, quizDto, sagaUnitOfWork);
                updateQuizFunctionalitySagas.executeWorkflow(sagaUnitOfWork);
                return updateQuizFunctionalitySagas.getUpdatedQuizDto();
            case TCC:
                CausalUnitOfWork causalUnitOfWork = causalUnitOfWorkService.createUnitOfWork(functionalityName);
                UpdateQuizFunctionalityTCC updateQuizFunctionalityTCC = new UpdateQuizFunctionalityTCC(
                        quizService, causalUnitOfWorkService, quizFactory, quizDto, causalUnitOfWork);
                updateQuizFunctionalityTCC.executeWorkflow(causalUnitOfWork);
                return updateQuizFunctionalityTCC.getUpdatedQuizDto();
            default: throw new TutorException(UNDEFINED_TRANSACTIONAL_MODEL);
        }
    }

}
