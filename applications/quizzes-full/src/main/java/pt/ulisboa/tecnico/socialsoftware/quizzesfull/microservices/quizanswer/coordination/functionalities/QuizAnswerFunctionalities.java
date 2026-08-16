package pt.ulisboa.tecnico.socialsoftware.quizzesfull.microservices.quizanswer.coordination.functionalities;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import pt.ulisboa.tecnico.socialsoftware.ms.messaging.CommandGateway;
import pt.ulisboa.tecnico.socialsoftware.ms.transaction.sagas.unitOfWork.SagaUnitOfWork;
import pt.ulisboa.tecnico.socialsoftware.ms.transaction.sagas.unitOfWork.SagaUnitOfWorkService;
import pt.ulisboa.tecnico.socialsoftware.quizzesfull.microservices.quizanswer.aggregate.QuizAnswerDto;
import pt.ulisboa.tecnico.socialsoftware.quizzesfull.microservices.quizanswer.coordination.sagas.AnswerQuestionFunctionalitySagas;
import pt.ulisboa.tecnico.socialsoftware.quizzesfull.microservices.quizanswer.coordination.sagas.ConcludeQuizFunctionalitySagas;
import pt.ulisboa.tecnico.socialsoftware.quizzesfull.microservices.quizanswer.coordination.sagas.CreateQuizAnswerFunctionalitySagas;
import pt.ulisboa.tecnico.socialsoftware.quizzesfull.microservices.quizanswer.coordination.sagas.GetQuizAnswerByQuizIdAndStudentIdFunctionalitySagas;
import pt.ulisboa.tecnico.socialsoftware.quizzesfull.microservices.quizanswer.service.QuizAnswerService;

@Service
public class QuizAnswerFunctionalities {

    @Autowired
    private SagaUnitOfWorkService unitOfWorkService;

    @Autowired
    private CommandGateway commandGateway;

    @Autowired
    private QuizAnswerService quizAnswerService;

    public QuizAnswerDto createQuizAnswer(Integer quizId, Integer userId) {
        String functionalityName = new Throwable().getStackTrace()[0].getMethodName();
        SagaUnitOfWork unitOfWork = unitOfWorkService.createUnitOfWork(functionalityName);
        CreateQuizAnswerFunctionalitySagas saga = new CreateQuizAnswerFunctionalitySagas(
                unitOfWorkService, quizId, userId, unitOfWork, commandGateway);
        saga.executeWorkflow(unitOfWork);
        return saga.getCreatedQuizAnswerDto();
    }

    public void answerQuestion(Integer quizAnswerId, Integer questionId, Integer optionKey, Integer timeTaken) {
        String functionalityName = new Throwable().getStackTrace()[0].getMethodName();
        SagaUnitOfWork unitOfWork = unitOfWorkService.createUnitOfWork(functionalityName);
        AnswerQuestionFunctionalitySagas saga = new AnswerQuestionFunctionalitySagas(
                unitOfWorkService, quizAnswerId, questionId, optionKey, timeTaken, unitOfWork, commandGateway);
        saga.executeWorkflow(unitOfWork);
    }

    public void concludeQuiz(Integer quizAnswerId) {
        String functionalityName = new Throwable().getStackTrace()[0].getMethodName();
        SagaUnitOfWork unitOfWork = unitOfWorkService.createUnitOfWork(functionalityName);
        ConcludeQuizFunctionalitySagas saga = new ConcludeQuizFunctionalitySagas(
                unitOfWorkService, quizAnswerId, unitOfWork, commandGateway);
        saga.executeWorkflow(unitOfWork);
    }

    public void removeQuizAnswerByEvent(Integer quizAnswerId) {
        SagaUnitOfWork unitOfWork = unitOfWorkService.createUnitOfWork("removeQuizAnswerByEvent");
        quizAnswerService.removeQuizAnswer(quizAnswerId, unitOfWork);
        unitOfWorkService.commit(unitOfWork);
    }

    public void removeQuizAnswerIfDisenrolledByEvent(Integer quizAnswerId, Integer userId) {
        SagaUnitOfWork unitOfWork = unitOfWorkService.createUnitOfWork("removeQuizAnswerIfDisenrolledByEvent");
        quizAnswerService.removeQuizAnswerIfUserMatches(quizAnswerId, userId, unitOfWork);
        unitOfWorkService.commit(unitOfWork);
    }

    public void updateStudentNameByEvent(Integer quizAnswerId, String name) {
        SagaUnitOfWork unitOfWork = unitOfWorkService.createUnitOfWork("updateStudentNameByEvent");
        quizAnswerService.updateStudentName(quizAnswerId, name, unitOfWork);
        unitOfWorkService.commit(unitOfWork);
    }

    public void anonymizeStudentByEvent(Integer quizAnswerId, String name, String username) {
        SagaUnitOfWork unitOfWork = unitOfWorkService.createUnitOfWork("anonymizeStudentByEvent");
        quizAnswerService.anonymizeStudent(quizAnswerId, name, username, unitOfWork);
        unitOfWorkService.commit(unitOfWork);
    }

    public void updateQuestionVersionByEvent(Integer quizAnswerId, Integer questionAggregateId, Long newVersion) {
        SagaUnitOfWork unitOfWork = unitOfWorkService.createUnitOfWork("updateQuestionVersionByEvent");
        quizAnswerService.updateQuestionVersionInQuizAnswer(quizAnswerId, questionAggregateId, newVersion, unitOfWork);
        unitOfWorkService.commit(unitOfWork);
    }

    public QuizAnswerDto getQuizAnswerByQuizIdAndStudentId(Integer quizId, Integer userId) {
        String functionalityName = new Throwable().getStackTrace()[0].getMethodName();
        SagaUnitOfWork unitOfWork = unitOfWorkService.createUnitOfWork(functionalityName);
        GetQuizAnswerByQuizIdAndStudentIdFunctionalitySagas saga = new GetQuizAnswerByQuizIdAndStudentIdFunctionalitySagas(
                unitOfWorkService, quizId, userId, unitOfWork, commandGateway);
        saga.executeWorkflow(unitOfWork);
        return saga.getQuizAnswerDto();
    }
}
