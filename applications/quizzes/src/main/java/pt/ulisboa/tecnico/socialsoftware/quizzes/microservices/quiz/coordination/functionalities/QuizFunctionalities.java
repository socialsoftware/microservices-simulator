package pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.quiz.coordination.functionalities;

import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Service;
import pt.ulisboa.tecnico.socialsoftware.ms.TransactionalModel;
import pt.ulisboa.tecnico.socialsoftware.ms.causal.unitOfWork.CausalUnitOfWork;
import pt.ulisboa.tecnico.socialsoftware.ms.causal.unitOfWork.CausalUnitOfWorkService;
import pt.ulisboa.tecnico.socialsoftware.ms.coordination.workflow.CommandGateway;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.unitOfWork.SagaUnitOfWork;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.unitOfWork.SagaUnitOfWorkService;
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.exception.QuizzesException;
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.quiz.aggregate.QuizDto;
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.quiz.coordination.causal.CreateQuizFunctionalityTCC;
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.quiz.coordination.causal.FindQuizFunctionalityTCC;
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.quiz.coordination.causal.GetAvailableQuizzesFunctionalityTCC;
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.quiz.coordination.causal.UpdateQuizFunctionalityTCC;
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.quiz.coordination.causal.RemoveCourseExecutionFromQuizFunctionalityTCC;
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.quiz.coordination.causal.UpdateQuestionInQuizFunctionalityTCC;
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.quiz.coordination.causal.RemoveQuizQuestionFunctionalityTCC;
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.quiz.coordination.sagas.CreateQuizFunctionalitySagas;
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.quiz.coordination.sagas.FindQuizFunctionalitySagas;
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.quiz.coordination.sagas.GetAvailableQuizzesFunctionalitySagas;
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.quiz.coordination.sagas.UpdateQuizFunctionalitySagas;
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.quiz.coordination.sagas.RemoveCourseExecutionFromQuizFunctionalitySagas;
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.quiz.coordination.sagas.UpdateQuestionInQuizFunctionalitySagas;
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.quiz.coordination.sagas.RemoveQuizQuestionFunctionalitySagas;
import pt.ulisboa.tecnico.socialsoftware.quizzes.events.DeleteCourseExecutionEvent;
import pt.ulisboa.tecnico.socialsoftware.quizzes.events.UpdateQuestionEvent;
import pt.ulisboa.tecnico.socialsoftware.quizzes.events.DeleteQuestionEvent;

import java.util.Arrays;
import java.util.List;

import static pt.ulisboa.tecnico.socialsoftware.ms.TransactionalModel.SAGAS;
import static pt.ulisboa.tecnico.socialsoftware.ms.TransactionalModel.TCC;
import static pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.exception.QuizzesErrorMessage.UNDEFINED_TRANSACTIONAL_MODEL;

@Service
public class QuizFunctionalities {
    @Autowired(required = false)
    private SagaUnitOfWorkService sagaUnitOfWorkService;
    @Autowired(required = false)
    private CausalUnitOfWorkService causalUnitOfWorkService;

    @Autowired
    private Environment env;

    private TransactionalModel workflowType;
    @Autowired
    private CommandGateway commandGateway;

    @PostConstruct
    public void init() {
        String[] activeProfiles = env.getActiveProfiles();
        if (Arrays.asList(activeProfiles).contains(SAGAS.getValue())) {
            workflowType = SAGAS;
        } else if (Arrays.asList(activeProfiles).contains(TCC.getValue())) {
            workflowType = TCC;
        } else {
            throw new QuizzesException(UNDEFINED_TRANSACTIONAL_MODEL);
        }
    }

    public QuizDto createQuiz(Integer courseExecutionId, QuizDto quizDto) throws Exception {
        String functionalityName = new Throwable().getStackTrace()[0].getMethodName();

        switch (workflowType) {
            case SAGAS:
                SagaUnitOfWork sagaUnitOfWork = sagaUnitOfWorkService.createUnitOfWork(functionalityName);
                CreateQuizFunctionalitySagas createQuizFunctionalitySagas = new CreateQuizFunctionalitySagas(
                        sagaUnitOfWorkService, courseExecutionId,
                        quizDto, sagaUnitOfWork, commandGateway);
                createQuizFunctionalitySagas.executeWorkflow(sagaUnitOfWork);
                return createQuizFunctionalitySagas.getCreatedQuizDto();
            case TCC:
                CausalUnitOfWork causalUnitOfWork = causalUnitOfWorkService.createUnitOfWork(functionalityName);
                CreateQuizFunctionalityTCC createQuizFunctionalityTCC = new CreateQuizFunctionalityTCC(
                        causalUnitOfWorkService, courseExecutionId, quizDto, causalUnitOfWork, commandGateway);
                createQuizFunctionalityTCC.executeWorkflow(causalUnitOfWork);
                return createQuizFunctionalityTCC.getCreatedQuizDto();
            default:
                throw new QuizzesException(UNDEFINED_TRANSACTIONAL_MODEL);
        }
    }

    public QuizDto findQuiz(Integer quizAggregateId) {
        String functionalityName = new Throwable().getStackTrace()[0].getMethodName();

        switch (workflowType) {
            case SAGAS:
                SagaUnitOfWork sagaUnitOfWork = sagaUnitOfWorkService.createUnitOfWork(functionalityName);
                FindQuizFunctionalitySagas findQuizFunctionalitySagas = new FindQuizFunctionalitySagas(
                        sagaUnitOfWorkService, quizAggregateId, sagaUnitOfWork, commandGateway);
                findQuizFunctionalitySagas.executeWorkflow(sagaUnitOfWork);
                return findQuizFunctionalitySagas.getQuizDto();
            case TCC:
                CausalUnitOfWork causalUnitOfWork = causalUnitOfWorkService.createUnitOfWork(functionalityName);
                FindQuizFunctionalityTCC findQuizFunctionalityTCC = new FindQuizFunctionalityTCC(
                        causalUnitOfWorkService, quizAggregateId, causalUnitOfWork, commandGateway);
                findQuizFunctionalityTCC.executeWorkflow(causalUnitOfWork);
                return findQuizFunctionalityTCC.getQuizDto();
            default:
                throw new QuizzesException(UNDEFINED_TRANSACTIONAL_MODEL);
        }
    }

    public List<QuizDto> getAvailableQuizzes(Integer userAggregateId, Integer courseExecutionAggregateId) {
        String functionalityName = new Throwable().getStackTrace()[0].getMethodName();

        switch (workflowType) {
            case SAGAS:
                SagaUnitOfWork sagaUnitOfWork = sagaUnitOfWorkService.createUnitOfWork(functionalityName);
                GetAvailableQuizzesFunctionalitySagas getAvailableQuizzesFunctionalitySagas = new GetAvailableQuizzesFunctionalitySagas(
                        sagaUnitOfWorkService, courseExecutionAggregateId, courseExecutionAggregateId,
                        sagaUnitOfWork, commandGateway);
                getAvailableQuizzesFunctionalitySagas.executeWorkflow(sagaUnitOfWork);
                return getAvailableQuizzesFunctionalitySagas.getAvailableQuizzes();
            case TCC:
                CausalUnitOfWork causalUnitOfWork = causalUnitOfWorkService.createUnitOfWork(functionalityName);
                GetAvailableQuizzesFunctionalityTCC getAvailableQuizzesFunctionalityTCC = new GetAvailableQuizzesFunctionalityTCC(
                        causalUnitOfWorkService, courseExecutionAggregateId, courseExecutionAggregateId,
                        causalUnitOfWork, commandGateway);
                getAvailableQuizzesFunctionalityTCC.executeWorkflow(causalUnitOfWork);
                return getAvailableQuizzesFunctionalityTCC.getAvailableQuizzes();
            default:
                throw new QuizzesException(UNDEFINED_TRANSACTIONAL_MODEL);
        }
    }

    public QuizDto updateQuiz(QuizDto quizDto) throws Exception {
        String functionalityName = new Throwable().getStackTrace()[0].getMethodName();

        switch (workflowType) {
            case SAGAS:
                SagaUnitOfWork sagaUnitOfWork = sagaUnitOfWorkService.createUnitOfWork(functionalityName);
                UpdateQuizFunctionalitySagas updateQuizFunctionalitySagas = new UpdateQuizFunctionalitySagas(
                        sagaUnitOfWorkService, quizDto, sagaUnitOfWork, commandGateway);
                updateQuizFunctionalitySagas.executeWorkflow(sagaUnitOfWork);
                return updateQuizFunctionalitySagas.getUpdatedQuizDto();
            case TCC:
                CausalUnitOfWork causalUnitOfWork = causalUnitOfWorkService.createUnitOfWork(functionalityName);
                UpdateQuizFunctionalityTCC updateQuizFunctionalityTCC = new UpdateQuizFunctionalityTCC(
                        causalUnitOfWorkService, quizDto, causalUnitOfWork, commandGateway);
                updateQuizFunctionalityTCC.executeWorkflow(causalUnitOfWork);
                return updateQuizFunctionalityTCC.getUpdatedQuizDto();
            default:
                throw new QuizzesException(UNDEFINED_TRANSACTIONAL_MODEL);
        }
    }

    public void removeCourseExecutionFromQuiz(Integer quizAggregateId, DeleteCourseExecutionEvent event) {
        String functionalityName = new Throwable().getStackTrace()[0].getMethodName();
        switch (workflowType) {
            case SAGAS:
                SagaUnitOfWork sagaUnitOfWork = sagaUnitOfWorkService.createUnitOfWork(functionalityName);
                RemoveCourseExecutionFromQuizFunctionalitySagas functionalitySagas = new RemoveCourseExecutionFromQuizFunctionalitySagas(
                        sagaUnitOfWorkService, quizAggregateId, event.getPublisherAggregateId(),
                        event.getPublisherAggregateVersion(), sagaUnitOfWork, commandGateway);
                functionalitySagas.executeWorkflow(sagaUnitOfWork);
                break;
            case TCC:
                CausalUnitOfWork causalUnitOfWork = causalUnitOfWorkService.createUnitOfWork(functionalityName);
                RemoveCourseExecutionFromQuizFunctionalityTCC functionalityTCC = new RemoveCourseExecutionFromQuizFunctionalityTCC(
                        causalUnitOfWorkService, quizAggregateId, event.getPublisherAggregateId(),
                        event.getPublisherAggregateVersion(), causalUnitOfWork, commandGateway);
                functionalityTCC.executeWorkflow(causalUnitOfWork);
                break;
            default:
                throw new QuizzesException(UNDEFINED_TRANSACTIONAL_MODEL);
        }
    }

    public void updateQuestionInQuiz(Integer quizAggregateId, UpdateQuestionEvent event) {
        String functionalityName = new Throwable().getStackTrace()[0].getMethodName();
        switch (workflowType) {
            case SAGAS:
                SagaUnitOfWork sagaUnitOfWork = sagaUnitOfWorkService.createUnitOfWork(functionalityName);
                UpdateQuestionInQuizFunctionalitySagas functionalitySagas = new UpdateQuestionInQuizFunctionalitySagas(
                        sagaUnitOfWorkService, quizAggregateId, event.getPublisherAggregateId(), event.getTitle(),
                        event.getContent(), event.getPublisherAggregateVersion(), sagaUnitOfWork, commandGateway);
                functionalitySagas.executeWorkflow(sagaUnitOfWork);
                break;
            case TCC:
                CausalUnitOfWork causalUnitOfWork = causalUnitOfWorkService.createUnitOfWork(functionalityName);
                UpdateQuestionInQuizFunctionalityTCC functionalityTCC = new UpdateQuestionInQuizFunctionalityTCC(
                        causalUnitOfWorkService, quizAggregateId, event.getPublisherAggregateId(), event.getTitle(),
                        event.getContent(), event.getPublisherAggregateVersion(), causalUnitOfWork, commandGateway);
                functionalityTCC.executeWorkflow(causalUnitOfWork);
                break;
            default:
                throw new QuizzesException(UNDEFINED_TRANSACTIONAL_MODEL);
        }
    }

    public void removeQuizQuestion(Integer quizAggregateId, DeleteQuestionEvent event) {
        String functionalityName = new Throwable().getStackTrace()[0].getMethodName();
        switch (workflowType) {
            case SAGAS:
                SagaUnitOfWork sagaUnitOfWork = sagaUnitOfWorkService.createUnitOfWork(functionalityName);
                RemoveQuizQuestionFunctionalitySagas functionalitySagas = new RemoveQuizQuestionFunctionalitySagas(
                        sagaUnitOfWorkService, quizAggregateId, event.getPublisherAggregateId(), sagaUnitOfWork,
                        commandGateway);
                functionalitySagas.executeWorkflow(sagaUnitOfWork);
                break;
            case TCC:
                CausalUnitOfWork causalUnitOfWork = causalUnitOfWorkService.createUnitOfWork(functionalityName);
                RemoveQuizQuestionFunctionalityTCC functionalityTCC = new RemoveQuizQuestionFunctionalityTCC(
                        causalUnitOfWorkService, quizAggregateId, event.getPublisherAggregateId(), causalUnitOfWork,
                        commandGateway);
                functionalityTCC.executeWorkflow(causalUnitOfWork);
                break;
            default:
                throw new QuizzesException(UNDEFINED_TRANSACTIONAL_MODEL);
        }
    }

}
