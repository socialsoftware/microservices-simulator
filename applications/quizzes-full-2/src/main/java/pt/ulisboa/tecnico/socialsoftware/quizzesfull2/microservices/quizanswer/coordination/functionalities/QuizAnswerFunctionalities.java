package pt.ulisboa.tecnico.socialsoftware.quizzesfull2.microservices.quizanswer.coordination.functionalities;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import pt.ulisboa.tecnico.socialsoftware.ms.messaging.CommandGateway;
import pt.ulisboa.tecnico.socialsoftware.ms.transaction.sagas.unitOfWork.SagaUnitOfWork;
import pt.ulisboa.tecnico.socialsoftware.ms.transaction.sagas.unitOfWork.SagaUnitOfWorkService;
import pt.ulisboa.tecnico.socialsoftware.quizzesfull2.microservices.quizanswer.aggregate.QuizAnswerDto;
import pt.ulisboa.tecnico.socialsoftware.quizzesfull2.microservices.quizanswer.coordination.sagas.GetQuizAnswerByIdFunctionalitySagas;
import pt.ulisboa.tecnico.socialsoftware.quizzesfull2.microservices.quizanswer.coordination.sagas.GetQuizAnswerForStudentAndQuizFunctionalitySagas;

@Service
public class QuizAnswerFunctionalities {
    @Autowired
    private SagaUnitOfWorkService unitOfWorkService;
    @Autowired
    private CommandGateway commandGateway;

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
}
