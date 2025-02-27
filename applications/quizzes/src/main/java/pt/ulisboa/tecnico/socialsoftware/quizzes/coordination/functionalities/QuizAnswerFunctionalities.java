package pt.ulisboa.tecnico.socialsoftware.ms.quizzes.coordination.functionalities;

import java.util.Arrays;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;
import pt.ulisboa.tecnico.socialsoftware.ms.MicroservicesSimulator;
import pt.ulisboa.tecnico.socialsoftware.ms.causal.unityOfWork.CausalUnitOfWork;
import pt.ulisboa.tecnico.socialsoftware.ms.causal.unityOfWork.CausalUnitOfWorkService;
import pt.ulisboa.tecnico.socialsoftware.ms.quizzes.causal.coordination.answer.AnswerQuestionFunctionalityTCC;
import pt.ulisboa.tecnico.socialsoftware.ms.quizzes.causal.coordination.answer.ConcludeQuizFunctionalityTCC;
import pt.ulisboa.tecnico.socialsoftware.ms.quizzes.causal.coordination.answer.StartQuizFunctionalityTCC;
import pt.ulisboa.tecnico.socialsoftware.ms.quizzes.microservices.answer.aggregate.QuestionAnswerDto;
import pt.ulisboa.tecnico.socialsoftware.ms.quizzes.microservices.answer.aggregate.QuizAnswerFactory;
import pt.ulisboa.tecnico.socialsoftware.ms.quizzes.microservices.answer.service.QuizAnswerService;
import pt.ulisboa.tecnico.socialsoftware.ms.quizzes.microservices.exception.TutorException;
import pt.ulisboa.tecnico.socialsoftware.ms.quizzes.microservices.question.service.QuestionService;
import pt.ulisboa.tecnico.socialsoftware.ms.quizzes.microservices.quiz.service.QuizService;
import pt.ulisboa.tecnico.socialsoftware.ms.quizzes.sagas.coordination.answer.AnswerQuestionFunctionalitySagas;
import pt.ulisboa.tecnico.socialsoftware.ms.quizzes.sagas.coordination.answer.ConcludeQuizFunctionalitySagas;
import pt.ulisboa.tecnico.socialsoftware.ms.quizzes.sagas.coordination.answer.StartQuizFunctionalitySagas;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.unityOfWork.SagaUnitOfWork;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.unityOfWork.SagaUnitOfWorkService;

import static pt.ulisboa.tecnico.socialsoftware.ms.MicroservicesSimulator.TransactionalModel.SAGAS;
import static pt.ulisboa.tecnico.socialsoftware.ms.MicroservicesSimulator.TransactionalModel.TCC;
import static pt.ulisboa.tecnico.socialsoftware.ms.quizzes.microservices.exception.ErrorMessage.UNDEFINED_TRANSACTIONAL_MODEL;

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

    public void answerQuestion(Integer quizAggregateId, Integer userAggregateId, QuestionAnswerDto userQuestionAnswerDto) {
        String functionalityName = new Throwable().getStackTrace()[0].getMethodName();

        switch (workflowType) {
            case SAGAS:
                SagaUnitOfWork sagaUnitOfWork = sagaUnitOfWorkService.createUnitOfWork(functionalityName);
                AnswerQuestionFunctionalitySagas answerQuestionFunctionalitySagas = new AnswerQuestionFunctionalitySagas(
                        quizAnswerService, questionService, sagaUnitOfWorkService, quizAnswerFactory, quizAggregateId, userAggregateId, userQuestionAnswerDto, sagaUnitOfWork);
                answerQuestionFunctionalitySagas.executeWorkflow(sagaUnitOfWork);
                break;
            case TCC:
                CausalUnitOfWork causalUnitOfWork = causalUnitOfWorkService.createUnitOfWork(functionalityName);
                AnswerQuestionFunctionalityTCC answerQuestionFunctionalityTCC = new AnswerQuestionFunctionalityTCC(
                        quizAnswerService, questionService, causalUnitOfWorkService, quizAnswerFactory, quizAggregateId, userAggregateId, userQuestionAnswerDto, causalUnitOfWork);
                answerQuestionFunctionalityTCC.executeWorkflow(causalUnitOfWork);
                break;
            default: throw new TutorException(UNDEFINED_TRANSACTIONAL_MODEL);
        }
    }

    public void startQuiz(Integer quizAggregateId, Integer courseExecutionAggregateId, Integer userAggregateId) {
        String functionalityName = new Throwable().getStackTrace()[0].getMethodName();

        switch (workflowType) {
            case SAGAS:
                SagaUnitOfWork sagaUnitOfWork = sagaUnitOfWorkService.createUnitOfWork(functionalityName);
                StartQuizFunctionalitySagas startQuizFunctionalitySagas = new StartQuizFunctionalitySagas(
                        quizAnswerService, quizService, sagaUnitOfWorkService, quizAggregateId, courseExecutionAggregateId, userAggregateId, sagaUnitOfWork);
                startQuizFunctionalitySagas.executeWorkflow(sagaUnitOfWork);
                break;
            case TCC:
                CausalUnitOfWork causalUnitOfWork = causalUnitOfWorkService.createUnitOfWork(functionalityName);
                StartQuizFunctionalityTCC startQuizFunctionalityTCC = new StartQuizFunctionalityTCC(
                        quizAnswerService, quizService, causalUnitOfWorkService, quizAggregateId, courseExecutionAggregateId, userAggregateId, causalUnitOfWork);
                startQuizFunctionalityTCC.executeWorkflow(causalUnitOfWork);
                break;
            default: throw new TutorException(UNDEFINED_TRANSACTIONAL_MODEL);
        }
    }

    public void concludeQuiz(Integer quizAggregateId, Integer userAggregateId) {
        String functionalityName = new Throwable().getStackTrace()[0].getMethodName();

        switch (workflowType) {
            case SAGAS:
                SagaUnitOfWork sagaUnitOfWork = sagaUnitOfWorkService.createUnitOfWork(functionalityName);
                ConcludeQuizFunctionalitySagas concludeQuizFunctionalitySagas = new ConcludeQuizFunctionalitySagas(
                        quizAnswerService, sagaUnitOfWorkService, quizAggregateId, userAggregateId, sagaUnitOfWork);
                concludeQuizFunctionalitySagas.executeWorkflow(sagaUnitOfWork);
                break;
            case TCC:
                CausalUnitOfWork causalUnitOfWork = causalUnitOfWorkService.createUnitOfWork(functionalityName);
                ConcludeQuizFunctionalityTCC concludeQuizFunctionalityTCC = new ConcludeQuizFunctionalityTCC(
                        quizAnswerService, causalUnitOfWorkService, quizAggregateId, userAggregateId, causalUnitOfWork);
                concludeQuizFunctionalityTCC.executeWorkflow(causalUnitOfWork);
                break;
            default: throw new TutorException(UNDEFINED_TRANSACTIONAL_MODEL);
        }
    }

}