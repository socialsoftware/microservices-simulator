package pt.ulisboa.tecnico.socialsoftware.quizzesfull2.microservices.quiz.coordination.functionalities;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import pt.ulisboa.tecnico.socialsoftware.ms.messaging.CommandGateway;
import pt.ulisboa.tecnico.socialsoftware.ms.transaction.sagas.aggregate.GenericSagaState;
import pt.ulisboa.tecnico.socialsoftware.ms.transaction.sagas.aggregate.SagaAggregate;
import pt.ulisboa.tecnico.socialsoftware.ms.transaction.sagas.unitOfWork.SagaUnitOfWork;
import pt.ulisboa.tecnico.socialsoftware.ms.transaction.sagas.unitOfWork.SagaUnitOfWorkService;
import pt.ulisboa.tecnico.socialsoftware.quizzesfull2.microservices.quiz.aggregate.QuizDto;
import pt.ulisboa.tecnico.socialsoftware.quizzesfull2.microservices.quiz.coordination.sagas.CreateQuizFunctionalitySagas;
import pt.ulisboa.tecnico.socialsoftware.quizzesfull2.microservices.quiz.coordination.sagas.GetQuizByIdFunctionalitySagas;
import pt.ulisboa.tecnico.socialsoftware.quizzesfull2.microservices.quiz.coordination.sagas.GetQuizzesForExecutionFunctionalitySagas;
import pt.ulisboa.tecnico.socialsoftware.quizzesfull2.microservices.quiz.coordination.sagas.UpdateQuizFunctionalitySagas;
import pt.ulisboa.tecnico.socialsoftware.quizzesfull2.microservices.quiz.service.QuizService;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class QuizFunctionalities {
    @Autowired
    private SagaUnitOfWorkService unitOfWorkService;
    @Autowired
    private CommandGateway commandGateway;
    @Autowired
    private QuizService quizService;

    public QuizDto getQuizById(Integer quizAggregateId) {
        SagaUnitOfWork unitOfWork = unitOfWorkService.createUnitOfWork("getQuizById");
        GetQuizByIdFunctionalitySagas saga = new GetQuizByIdFunctionalitySagas(
                unitOfWorkService, quizAggregateId, unitOfWork, commandGateway);
        saga.executeWorkflow(unitOfWork);
        return saga.getQuizDto();
    }

    public List<QuizDto> getQuizzesForExecution(Integer executionAggregateId) {
        SagaUnitOfWork unitOfWork = unitOfWorkService.createUnitOfWork("getQuizzesForExecution");
        GetQuizzesForExecutionFunctionalitySagas saga = new GetQuizzesForExecutionFunctionalitySagas(
                unitOfWorkService, executionAggregateId, unitOfWork, commandGateway);
        saga.executeWorkflow(unitOfWork);
        return saga.getQuizzes();
    }

    public QuizDto createQuiz(QuizDto quizDto, List<Integer> questionAggregateIds) {
        SagaUnitOfWork unitOfWork = unitOfWorkService.createUnitOfWork("createQuiz");
        CreateQuizFunctionalitySagas saga = new CreateQuizFunctionalitySagas(
                unitOfWorkService, quizDto, questionAggregateIds, unitOfWork, commandGateway);
        saga.executeWorkflow(unitOfWork);
        return saga.getQuizDto();
    }

    public void updateQuiz(Integer quizAggregateId, String title, LocalDateTime availableDate,
                           LocalDateTime conclusionDate, LocalDateTime resultsDate,
                           List<Integer> questionAggregateIds) {
        SagaUnitOfWork unitOfWork = unitOfWorkService.createUnitOfWork("updateQuiz");
        UpdateQuizFunctionalitySagas saga = new UpdateQuizFunctionalitySagas(
                unitOfWorkService, quizAggregateId, title, availableDate, conclusionDate, resultsDate,
                questionAggregateIds, unitOfWork, commandGateway);
        saga.executeWorkflow(unitOfWork);
    }

    public void setQuestionDetailsByEvent(Integer quizAggregateId, Integer questionAggregateId, String title,
                                          String content, Long questionVersion) {
        SagaUnitOfWork unitOfWork = unitOfWorkService.createUnitOfWork("setQuestionDetailsByEvent");
        if (isInSaga(quizAggregateId, unitOfWork)) {
            return;
        }
        quizService.setQuestionDetails(quizAggregateId, questionAggregateId, title, content, questionVersion,
                unitOfWork);
        unitOfWorkService.commit(unitOfWork);
    }

    public void invalidateForDeletedQuestionByEvent(Integer quizAggregateId, Integer questionAggregateId) {
        SagaUnitOfWork unitOfWork = unitOfWorkService.createUnitOfWork("invalidateForDeletedQuestionByEvent");
        if (isInSaga(quizAggregateId, unitOfWork)) {
            return;
        }
        quizService.invalidateForDeletedQuestion(quizAggregateId, questionAggregateId, unitOfWork);
        unitOfWorkService.commit(unitOfWork);
    }

    public void removeForDeletedExecutionByEvent(Integer quizAggregateId, Integer executionAggregateId) {
        SagaUnitOfWork unitOfWork = unitOfWorkService.createUnitOfWork("removeForDeletedExecutionByEvent");
        if (isInSaga(quizAggregateId, unitOfWork)) {
            return;
        }
        quizService.removeForDeletedExecution(quizAggregateId, executionAggregateId, unitOfWork);
        unitOfWorkService.commit(unitOfWork);
    }

    // The guard belongs here rather than in the service methods, which the saga steps also call.
    private boolean isInSaga(Integer quizAggregateId, SagaUnitOfWork unitOfWork) {
        SagaAggregate quiz = (SagaAggregate) unitOfWorkService.aggregateLoadAndRegisterRead(
                quizAggregateId, unitOfWork);
        return !GenericSagaState.NOT_IN_SAGA.equals(quiz.getSagaState());
    }
}
