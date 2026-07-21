package pt.ulisboa.tecnico.socialsoftware.quizzesfull.microservices.quiz.coordination.functionalities;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import pt.ulisboa.tecnico.socialsoftware.ms.messaging.CommandGateway;
import pt.ulisboa.tecnico.socialsoftware.ms.transaction.sagas.unitOfWork.SagaUnitOfWork;
import pt.ulisboa.tecnico.socialsoftware.ms.transaction.sagas.unitOfWork.SagaUnitOfWorkService;
import pt.ulisboa.tecnico.socialsoftware.quizzesfull.microservices.quiz.aggregate.QuizDto;
import pt.ulisboa.tecnico.socialsoftware.quizzesfull.microservices.quiz.coordination.sagas.CreateQuizFunctionalitySagas;
import pt.ulisboa.tecnico.socialsoftware.quizzesfull.microservices.quiz.coordination.sagas.GetQuizByIdFunctionalitySagas;
import pt.ulisboa.tecnico.socialsoftware.quizzesfull.microservices.quiz.coordination.sagas.UpdateQuizFunctionalitySagas;
import pt.ulisboa.tecnico.socialsoftware.quizzesfull.microservices.quiz.service.QuizService;

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

    public QuizDto createQuiz(String title, LocalDateTime availableDate, LocalDateTime conclusionDate,
                               LocalDateTime resultsDate, String quizType, Integer executionId,
                               List<Integer> questionIds) {
        String functionalityName = new Throwable().getStackTrace()[0].getMethodName();
        SagaUnitOfWork unitOfWork = unitOfWorkService.createUnitOfWork(functionalityName);
        CreateQuizFunctionalitySagas saga = new CreateQuizFunctionalitySagas(
                unitOfWorkService, title, availableDate, conclusionDate, resultsDate,
                quizType, executionId, questionIds, unitOfWork, commandGateway);
        saga.executeWorkflow(unitOfWork);
        return saga.getCreatedQuizDto();
    }

    public void updateQuiz(Integer quizId, LocalDateTime availableDate, LocalDateTime conclusionDate,
                            LocalDateTime resultsDate, List<Integer> questionIds) {
        String functionalityName = new Throwable().getStackTrace()[0].getMethodName();
        SagaUnitOfWork unitOfWork = unitOfWorkService.createUnitOfWork(functionalityName);
        UpdateQuizFunctionalitySagas saga = new UpdateQuizFunctionalitySagas(
                unitOfWorkService, quizId, availableDate, conclusionDate, resultsDate,
                questionIds, unitOfWork, commandGateway);
        saga.executeWorkflow(unitOfWork);
    }

    public QuizDto getQuizById(Integer quizAggregateId) {
        String functionalityName = new Throwable().getStackTrace()[0].getMethodName();
        SagaUnitOfWork unitOfWork = unitOfWorkService.createUnitOfWork(functionalityName);
        GetQuizByIdFunctionalitySagas saga = new GetQuizByIdFunctionalitySagas(
                unitOfWorkService, quizAggregateId, unitOfWork, commandGateway);
        saga.executeWorkflow(unitOfWork);
        return saga.getQuizDto();
    }

    public void updateQuestionInQuizByEvent(Integer quizId, Integer questionAggregateId, String title, String content) {
        SagaUnitOfWork unitOfWork = unitOfWorkService.createUnitOfWork("updateQuestionInQuizByEvent");
        quizService.updateQuestionInQuiz(quizId, questionAggregateId, title, content, unitOfWork);
        unitOfWorkService.commit(unitOfWork);
    }

    public void removeQuestionFromQuizByEvent(Integer quizId, Integer questionAggregateId) {
        SagaUnitOfWork unitOfWork = unitOfWorkService.createUnitOfWork("removeQuestionFromQuizByEvent");
        quizService.removeQuestionFromQuiz(quizId, questionAggregateId, unitOfWork);
        unitOfWorkService.commit(unitOfWork);
    }

    public void invalidateQuizByEvent(Integer quizId) {
        SagaUnitOfWork unitOfWork = unitOfWorkService.createUnitOfWork("invalidateQuizByEvent");
        quizService.invalidateQuiz(quizId, unitOfWork);
        unitOfWorkService.commit(unitOfWork);
    }
}
