package pt.ulisboa.tecnico.socialsoftware.quizzesfull2.microservices.quiz.coordination.functionalities;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import pt.ulisboa.tecnico.socialsoftware.ms.messaging.CommandGateway;
import pt.ulisboa.tecnico.socialsoftware.ms.transaction.sagas.unitOfWork.SagaUnitOfWork;
import pt.ulisboa.tecnico.socialsoftware.ms.transaction.sagas.unitOfWork.SagaUnitOfWorkService;
import pt.ulisboa.tecnico.socialsoftware.quizzesfull2.microservices.quiz.aggregate.QuizDto;
import pt.ulisboa.tecnico.socialsoftware.quizzesfull2.microservices.quiz.coordination.sagas.CreateQuizFunctionalitySagas;
import pt.ulisboa.tecnico.socialsoftware.quizzesfull2.microservices.quiz.coordination.sagas.GetQuizByIdFunctionalitySagas;
import pt.ulisboa.tecnico.socialsoftware.quizzesfull2.microservices.quiz.coordination.sagas.GetQuizzesForExecutionFunctionalitySagas;
import pt.ulisboa.tecnico.socialsoftware.quizzesfull2.microservices.quiz.coordination.sagas.UpdateQuizFunctionalitySagas;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class QuizFunctionalities {
    @Autowired
    private SagaUnitOfWorkService unitOfWorkService;
    @Autowired
    private CommandGateway commandGateway;

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
}
