package pt.ulisboa.tecnico.socialsoftware.quizzesfull2.microservices.quizanswer.coordination.functionalities;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import pt.ulisboa.tecnico.socialsoftware.ms.messaging.CommandGateway;
import pt.ulisboa.tecnico.socialsoftware.ms.transaction.sagas.aggregate.GenericSagaState;
import pt.ulisboa.tecnico.socialsoftware.ms.transaction.sagas.aggregate.SagaAggregate;
import pt.ulisboa.tecnico.socialsoftware.ms.transaction.sagas.unitOfWork.SagaUnitOfWork;
import pt.ulisboa.tecnico.socialsoftware.ms.transaction.sagas.unitOfWork.SagaUnitOfWorkService;
import pt.ulisboa.tecnico.socialsoftware.quizzesfull2.microservices.quizanswer.aggregate.QuizAnswerDto;
import pt.ulisboa.tecnico.socialsoftware.quizzesfull2.microservices.quizanswer.coordination.sagas.AnswerQuestionFunctionalitySagas;
import pt.ulisboa.tecnico.socialsoftware.quizzesfull2.microservices.quizanswer.coordination.sagas.ConcludeQuizFunctionalitySagas;
import pt.ulisboa.tecnico.socialsoftware.quizzesfull2.microservices.quizanswer.coordination.sagas.CreateQuizAnswerFunctionalitySagas;
import pt.ulisboa.tecnico.socialsoftware.quizzesfull2.microservices.quizanswer.coordination.sagas.GetQuizAnswerByIdFunctionalitySagas;
import pt.ulisboa.tecnico.socialsoftware.quizzesfull2.microservices.quizanswer.coordination.sagas.GetQuizAnswerForStudentAndQuizFunctionalitySagas;
import pt.ulisboa.tecnico.socialsoftware.quizzesfull2.microservices.quizanswer.service.QuizAnswerService;

@Service
public class QuizAnswerFunctionalities {
    @Autowired
    private SagaUnitOfWorkService unitOfWorkService;
    @Autowired
    private CommandGateway commandGateway;
    @Autowired
    private QuizAnswerService quizAnswerService;

    public QuizAnswerDto getQuizAnswerById(Integer quizAnswerAggregateId) {
        SagaUnitOfWork unitOfWork = unitOfWorkService.createUnitOfWork("getQuizAnswerById");
        GetQuizAnswerByIdFunctionalitySagas saga = new GetQuizAnswerByIdFunctionalitySagas(
                unitOfWorkService, quizAnswerAggregateId, unitOfWork, commandGateway);
        saga.executeWorkflow(unitOfWork);
        return saga.getQuizAnswerDto();
    }

    public QuizAnswerDto getQuizAnswerForStudentAndQuiz(Integer userAggregateId, Integer quizAggregateId) {
        SagaUnitOfWork unitOfWork = unitOfWorkService.createUnitOfWork("getQuizAnswerForStudentAndQuiz");
        GetQuizAnswerForStudentAndQuizFunctionalitySagas saga = new GetQuizAnswerForStudentAndQuizFunctionalitySagas(
                unitOfWorkService, userAggregateId, quizAggregateId, unitOfWork, commandGateway);
        saga.executeWorkflow(unitOfWork);
        return saga.getQuizAnswerDto();
    }

    public QuizAnswerDto createQuizAnswer(Integer quizAggregateId, Integer userAggregateId,
                                          Integer executionAggregateId) {
        SagaUnitOfWork unitOfWork = unitOfWorkService.createUnitOfWork("createQuizAnswer");
        CreateQuizAnswerFunctionalitySagas saga = new CreateQuizAnswerFunctionalitySagas(
                unitOfWorkService, quizAggregateId, userAggregateId, executionAggregateId, unitOfWork,
                commandGateway);
        saga.executeWorkflow(unitOfWork);
        return saga.getQuizAnswerDto();
    }

    public void answerQuestion(Integer quizAnswerAggregateId, Integer questionAggregateId,
                               Integer optionSequenceChoice, Integer optionKey, Integer timeTaken) {
        SagaUnitOfWork unitOfWork = unitOfWorkService.createUnitOfWork("answerQuestion");
        AnswerQuestionFunctionalitySagas saga = new AnswerQuestionFunctionalitySagas(
                unitOfWorkService, quizAnswerAggregateId, questionAggregateId, optionSequenceChoice, optionKey,
                timeTaken, unitOfWork, commandGateway);
        saga.executeWorkflow(unitOfWork);
    }

    public void concludeQuiz(Integer quizAnswerAggregateId) {
        SagaUnitOfWork unitOfWork = unitOfWorkService.createUnitOfWork("concludeQuiz");
        ConcludeQuizFunctionalitySagas saga = new ConcludeQuizFunctionalitySagas(
                unitOfWorkService, quizAnswerAggregateId, unitOfWork, commandGateway);
        saga.executeWorkflow(unitOfWork);
    }

    public void setStudentNameByEvent(Integer quizAnswerAggregateId, Integer userAggregateId, String userName,
                                      Long userVersion) {
        SagaUnitOfWork unitOfWork = unitOfWorkService.createUnitOfWork("setStudentNameByEvent");
        if (isInSaga(quizAnswerAggregateId, unitOfWork)) {
            return;
        }
        quizAnswerService.setStudentName(quizAnswerAggregateId, userAggregateId, userName, userVersion,
                unitOfWork);
        unitOfWorkService.commit(unitOfWork);
    }

    // The student snapshot caches no username, so anonymizing it is exactly the name replacement
    // setStudentName already performs.
    public void anonymizeStudentByEvent(Integer quizAnswerAggregateId, Integer userAggregateId, String userName,
                                        Long userVersion) {
        SagaUnitOfWork unitOfWork = unitOfWorkService.createUnitOfWork("anonymizeStudentByEvent");
        if (isInSaga(quizAnswerAggregateId, unitOfWork)) {
            return;
        }
        quizAnswerService.setStudentName(quizAnswerAggregateId, userAggregateId, userName, userVersion,
                unitOfWork);
        unitOfWorkService.commit(unitOfWork);
    }

    public void removeForDeletedStudentByEvent(Integer quizAnswerAggregateId, Integer userAggregateId) {
        SagaUnitOfWork unitOfWork = unitOfWorkService.createUnitOfWork("removeForDeletedStudentByEvent");
        if (isInSaga(quizAnswerAggregateId, unitOfWork)) {
            return;
        }
        quizAnswerService.removeForDeletedStudent(quizAnswerAggregateId, userAggregateId, unitOfWork);
        unitOfWorkService.commit(unitOfWork);
    }

    public void setQuestionVersionByEvent(Integer quizAnswerAggregateId, Integer questionAggregateId,
                                          Long questionVersion) {
        SagaUnitOfWork unitOfWork = unitOfWorkService.createUnitOfWork("setQuestionVersionByEvent");
        if (isInSaga(quizAnswerAggregateId, unitOfWork)) {
            return;
        }
        quizAnswerService.setQuestionVersion(quizAnswerAggregateId, questionAggregateId, questionVersion,
                unitOfWork);
        unitOfWorkService.commit(unitOfWork);
    }

    public void removeForDeletedExecutionByEvent(Integer quizAnswerAggregateId, Integer executionAggregateId) {
        SagaUnitOfWork unitOfWork = unitOfWorkService.createUnitOfWork("removeForDeletedExecutionByEvent");
        if (isInSaga(quizAnswerAggregateId, unitOfWork)) {
            return;
        }
        quizAnswerService.removeForDeletedExecution(quizAnswerAggregateId, executionAggregateId, unitOfWork);
        unitOfWorkService.commit(unitOfWork);
    }

    public void removeForDisenrolledStudentByEvent(Integer quizAnswerAggregateId, Integer executionAggregateId,
                                                   Integer userAggregateId) {
        SagaUnitOfWork unitOfWork = unitOfWorkService.createUnitOfWork("removeForDisenrolledStudentByEvent");
        if (isInSaga(quizAnswerAggregateId, unitOfWork)) {
            return;
        }
        quizAnswerService.removeForDisenrolledStudent(quizAnswerAggregateId, executionAggregateId,
                userAggregateId, unitOfWork);
        unitOfWorkService.commit(unitOfWork);
    }

    public void removeForInvalidatedQuizByEvent(Integer quizAnswerAggregateId, Integer quizAggregateId) {
        SagaUnitOfWork unitOfWork = unitOfWorkService.createUnitOfWork("removeForInvalidatedQuizByEvent");
        if (isInSaga(quizAnswerAggregateId, unitOfWork)) {
            return;
        }
        quizAnswerService.removeForInvalidatedQuiz(quizAnswerAggregateId, quizAggregateId, unitOfWork);
        unitOfWorkService.commit(unitOfWork);
    }

    // The guard belongs here rather than in the service methods, which the saga steps also call.
    private boolean isInSaga(Integer quizAnswerAggregateId, SagaUnitOfWork unitOfWork) {
        SagaAggregate quizAnswer = (SagaAggregate) unitOfWorkService.aggregateLoadAndRegisterRead(
                quizAnswerAggregateId, unitOfWork);
        return !GenericSagaState.NOT_IN_SAGA.equals(quizAnswer.getSagaState());
    }
}
