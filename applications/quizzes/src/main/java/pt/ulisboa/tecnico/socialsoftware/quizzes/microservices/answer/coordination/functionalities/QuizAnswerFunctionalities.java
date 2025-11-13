package pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.answer.coordination.functionalities;

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
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.answer.aggregate.QuestionAnswerDto;
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.answer.aggregate.QuizAnswerFactory;
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.answer.coordination.causal.AnswerQuestionFunctionalityTCC;
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.answer.coordination.causal.ConcludeQuizFunctionalityTCC;
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.answer.coordination.causal.StartQuizFunctionalityTCC;
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.answer.coordination.causal.RemoveUserFromQuizAnswerFunctionalityTCC;
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.answer.coordination.causal.RemoveQuestionFromQuizAnswerFunctionalityTCC;
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.answer.coordination.causal.UpdateUserNameInQuizAnswerFunctionalityTCC;
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.answer.coordination.sagas.AnswerQuestionFunctionalitySagas;
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.answer.coordination.sagas.ConcludeQuizFunctionalitySagas;
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.answer.coordination.sagas.StartQuizFunctionalitySagas;
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.answer.coordination.sagas.RemoveUserFromQuizAnswerFunctionalitySagas;
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.answer.coordination.sagas.RemoveQuestionFromQuizAnswerFunctionalitySagas;
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.answer.coordination.sagas.UpdateUserNameInQuizAnswerFunctionalitySagas;
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.exception.QuizzesException;

import java.util.Arrays;

import static pt.ulisboa.tecnico.socialsoftware.ms.TransactionalModel.SAGAS;
import static pt.ulisboa.tecnico.socialsoftware.ms.TransactionalModel.TCC;
import static pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.exception.QuizzesErrorMessage.UNDEFINED_TRANSACTIONAL_MODEL;

@Service
public class QuizAnswerFunctionalities {
    @Autowired(required = false)
    private SagaUnitOfWorkService sagaUnitOfWorkService;
    @Autowired(required = false)
    private CausalUnitOfWorkService causalUnitOfWorkService;
    @Autowired
    private QuizAnswerFactory quizAnswerFactory;
    @Autowired
    private CommandGateway commandGateway;

    @Autowired
    private Environment env;

    private TransactionalModel workflowType;

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

    public void answerQuestion(Integer quizAggregateId, Integer userAggregateId,
            QuestionAnswerDto userQuestionAnswerDto) {
        String functionalityName = new Throwable().getStackTrace()[0].getMethodName();

        switch (workflowType) {
            case SAGAS:
                SagaUnitOfWork sagaUnitOfWork = sagaUnitOfWorkService.createUnitOfWork(functionalityName);
                AnswerQuestionFunctionalitySagas answerQuestionFunctionalitySagas = new AnswerQuestionFunctionalitySagas(
                        sagaUnitOfWorkService, quizAnswerFactory, quizAggregateId,
                        userAggregateId, userQuestionAnswerDto, sagaUnitOfWork, commandGateway);
                answerQuestionFunctionalitySagas.executeWorkflow(sagaUnitOfWork);
                break;
            case TCC:
                CausalUnitOfWork causalUnitOfWork = causalUnitOfWorkService.createUnitOfWork(functionalityName);
                AnswerQuestionFunctionalityTCC answerQuestionFunctionalityTCC = new AnswerQuestionFunctionalityTCC(
                        causalUnitOfWorkService, quizAnswerFactory, quizAggregateId,
                        userAggregateId, userQuestionAnswerDto, causalUnitOfWork, commandGateway);
                answerQuestionFunctionalityTCC.executeWorkflow(causalUnitOfWork);
                break;
            default:
                throw new QuizzesException(UNDEFINED_TRANSACTIONAL_MODEL);
        }
    }

    public void startQuiz(Integer quizAggregateId, Integer courseExecutionAggregateId, Integer userAggregateId) {
        String functionalityName = new Throwable().getStackTrace()[0].getMethodName();

        switch (workflowType) {
            case SAGAS:
                SagaUnitOfWork sagaUnitOfWork = sagaUnitOfWorkService.createUnitOfWork(functionalityName);
                StartQuizFunctionalitySagas startQuizFunctionalitySagas = new StartQuizFunctionalitySagas(
                        sagaUnitOfWorkService, quizAggregateId,
                        courseExecutionAggregateId, userAggregateId, sagaUnitOfWork, commandGateway);
                startQuizFunctionalitySagas.executeWorkflow(sagaUnitOfWork);
                break;
            case TCC:
                CausalUnitOfWork causalUnitOfWork = causalUnitOfWorkService.createUnitOfWork(functionalityName);
                StartQuizFunctionalityTCC startQuizFunctionalityTCC = new StartQuizFunctionalityTCC(
                        causalUnitOfWorkService, quizAggregateId,
                        courseExecutionAggregateId, userAggregateId, causalUnitOfWork, commandGateway);
                startQuizFunctionalityTCC.executeWorkflow(causalUnitOfWork);
                break;
            default:
                throw new QuizzesException(UNDEFINED_TRANSACTIONAL_MODEL);
        }
    }

    public void concludeQuiz(Integer quizAggregateId, Integer userAggregateId) {
        String functionalityName = new Throwable().getStackTrace()[0].getMethodName();

        switch (workflowType) {
            case SAGAS:
                SagaUnitOfWork sagaUnitOfWork = sagaUnitOfWorkService.createUnitOfWork(functionalityName);
                ConcludeQuizFunctionalitySagas concludeQuizFunctionalitySagas = new ConcludeQuizFunctionalitySagas(
                        sagaUnitOfWorkService, quizAggregateId, userAggregateId, sagaUnitOfWork,
                        commandGateway);
                concludeQuizFunctionalitySagas.executeWorkflow(sagaUnitOfWork);
                break;
            case TCC:
                CausalUnitOfWork causalUnitOfWork = causalUnitOfWorkService.createUnitOfWork(functionalityName);
                ConcludeQuizFunctionalityTCC concludeQuizFunctionalityTCC = new ConcludeQuizFunctionalityTCC(
                        causalUnitOfWorkService, quizAggregateId, userAggregateId, causalUnitOfWork,
                        commandGateway);
                concludeQuizFunctionalityTCC.executeWorkflow(causalUnitOfWork);
                break;
            default:
                throw new QuizzesException(UNDEFINED_TRANSACTIONAL_MODEL);
        }
    }

    public void removeUserFromQuizAnswer(Integer quizAnswerAggregateId, Integer userAggregateId,
            Integer publisherAggregateVersion) {
        String functionalityName = new Throwable().getStackTrace()[0].getMethodName();
        switch (workflowType) {
            case SAGAS:
                SagaUnitOfWork sagaUnitOfWork = sagaUnitOfWorkService.createUnitOfWork(functionalityName);
                RemoveUserFromQuizAnswerFunctionalitySagas functionalitySagas = new RemoveUserFromQuizAnswerFunctionalitySagas(
                        sagaUnitOfWorkService, quizAnswerAggregateId, userAggregateId, publisherAggregateVersion,
                        sagaUnitOfWork, commandGateway);
                functionalitySagas.executeWorkflow(sagaUnitOfWork);
                break;
            case TCC:
                CausalUnitOfWork causalUnitOfWork = causalUnitOfWorkService.createUnitOfWork(functionalityName);
                RemoveUserFromQuizAnswerFunctionalityTCC functionalityTCC = new RemoveUserFromQuizAnswerFunctionalityTCC(
                        causalUnitOfWorkService, quizAnswerAggregateId, userAggregateId, publisherAggregateVersion,
                        causalUnitOfWork, commandGateway);
                functionalityTCC.executeWorkflow(causalUnitOfWork);
                break;
            default:
                throw new QuizzesException(UNDEFINED_TRANSACTIONAL_MODEL);
        }
    }

    public void removeQuestionFromQuizAnswer(Integer quizAnswerAggregateId, Integer questionAggregateId,
            Integer publisherAggregateVersion) {
        String functionalityName = new Throwable().getStackTrace()[0].getMethodName();
        switch (workflowType) {
            case SAGAS:
                SagaUnitOfWork sagaUnitOfWork = sagaUnitOfWorkService.createUnitOfWork(functionalityName);
                RemoveQuestionFromQuizAnswerFunctionalitySagas functionalitySagas = new RemoveQuestionFromQuizAnswerFunctionalitySagas(
                        sagaUnitOfWorkService, quizAnswerAggregateId, questionAggregateId, publisherAggregateVersion,
                        sagaUnitOfWork, commandGateway);
                functionalitySagas.executeWorkflow(sagaUnitOfWork);
                break;
            case TCC:
                CausalUnitOfWork causalUnitOfWork = causalUnitOfWorkService.createUnitOfWork(functionalityName);
                RemoveQuestionFromQuizAnswerFunctionalityTCC functionalityTCC = new RemoveQuestionFromQuizAnswerFunctionalityTCC(
                        causalUnitOfWorkService, quizAnswerAggregateId, questionAggregateId, publisherAggregateVersion,
                        causalUnitOfWork, commandGateway);
                functionalityTCC.executeWorkflow(causalUnitOfWork);
                break;
            default:
                throw new QuizzesException(UNDEFINED_TRANSACTIONAL_MODEL);
        }
    }

    public void updateUserNameInQuizAnswer(Integer quizAnswerAggregateId, Integer publisherAggregateId,
            Integer publisherAggregateVersion, Integer studentAggregateId, String updatedName) {
        String functionalityName = new Throwable().getStackTrace()[0].getMethodName();
        switch (workflowType) {
            case SAGAS:
                SagaUnitOfWork sagaUnitOfWork = sagaUnitOfWorkService.createUnitOfWork(functionalityName);
                UpdateUserNameInQuizAnswerFunctionalitySagas functionalitySagas = new UpdateUserNameInQuizAnswerFunctionalitySagas(
                        sagaUnitOfWorkService, quizAnswerAggregateId, publisherAggregateId, publisherAggregateVersion,
                        studentAggregateId, updatedName, sagaUnitOfWork, commandGateway);
                functionalitySagas.executeWorkflow(sagaUnitOfWork);
                break;
            case TCC:
                CausalUnitOfWork causalUnitOfWork = causalUnitOfWorkService.createUnitOfWork(functionalityName);
                UpdateUserNameInQuizAnswerFunctionalityTCC functionalityTCC = new UpdateUserNameInQuizAnswerFunctionalityTCC(
                        causalUnitOfWorkService, quizAnswerAggregateId, publisherAggregateId, publisherAggregateVersion,
                        studentAggregateId, updatedName, causalUnitOfWork, commandGateway);
                functionalityTCC.executeWorkflow(causalUnitOfWork);
                break;
            default:
                throw new QuizzesException(UNDEFINED_TRANSACTIONAL_MODEL);
        }
    }

}