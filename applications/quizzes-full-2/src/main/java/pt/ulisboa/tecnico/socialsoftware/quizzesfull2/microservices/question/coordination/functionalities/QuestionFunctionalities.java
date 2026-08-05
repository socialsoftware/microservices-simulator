package pt.ulisboa.tecnico.socialsoftware.quizzesfull2.microservices.question.coordination.functionalities;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import pt.ulisboa.tecnico.socialsoftware.ms.messaging.CommandGateway;
import pt.ulisboa.tecnico.socialsoftware.ms.transaction.sagas.unitOfWork.SagaUnitOfWork;
import pt.ulisboa.tecnico.socialsoftware.ms.transaction.sagas.unitOfWork.SagaUnitOfWorkService;
import pt.ulisboa.tecnico.socialsoftware.quizzesfull2.microservices.question.aggregate.QuestionDto;
import pt.ulisboa.tecnico.socialsoftware.quizzesfull2.microservices.question.coordination.sagas.CreateQuestionFunctionalitySagas;
import pt.ulisboa.tecnico.socialsoftware.quizzesfull2.microservices.question.coordination.sagas.DeleteQuestionFunctionalitySagas;
import pt.ulisboa.tecnico.socialsoftware.quizzesfull2.microservices.question.coordination.sagas.GetQuestionByIdFunctionalitySagas;
import pt.ulisboa.tecnico.socialsoftware.quizzesfull2.microservices.question.coordination.sagas.GetQuestionsByCourseFunctionalitySagas;
import pt.ulisboa.tecnico.socialsoftware.quizzesfull2.microservices.question.coordination.sagas.UpdateQuestionFunctionalitySagas;

import java.util.List;

@Service
public class QuestionFunctionalities {
    @Autowired
    private SagaUnitOfWorkService unitOfWorkService;
    @Autowired
    private CommandGateway commandGateway;

    public QuestionDto getQuestionById(Integer questionAggregateId) {
        SagaUnitOfWork unitOfWork = unitOfWorkService.createUnitOfWork("getQuestionById");
        GetQuestionByIdFunctionalitySagas saga = new GetQuestionByIdFunctionalitySagas(
                unitOfWorkService, questionAggregateId, unitOfWork, commandGateway);
        saga.executeWorkflow(unitOfWork);
        return saga.getQuestionDto();
    }

    public List<QuestionDto> getQuestionsByCourse(Integer courseAggregateId) {
        SagaUnitOfWork unitOfWork = unitOfWorkService.createUnitOfWork("getQuestionsByCourse");
        GetQuestionsByCourseFunctionalitySagas saga = new GetQuestionsByCourseFunctionalitySagas(
                unitOfWorkService, courseAggregateId, unitOfWork, commandGateway);
        saga.executeWorkflow(unitOfWork);
        return saga.getQuestions();
    }

    public QuestionDto createQuestion(QuestionDto questionDto, List<Integer> topicAggregateIds) {
        SagaUnitOfWork unitOfWork = unitOfWorkService.createUnitOfWork("createQuestion");
        CreateQuestionFunctionalitySagas saga = new CreateQuestionFunctionalitySagas(
                unitOfWorkService, questionDto, topicAggregateIds, unitOfWork, commandGateway);
        saga.executeWorkflow(unitOfWork);
        return saga.getQuestionDto();
    }

    public void updateQuestion(Integer questionAggregateId, String title, String content,
                               List<Integer> topicAggregateIds) {
        SagaUnitOfWork unitOfWork = unitOfWorkService.createUnitOfWork("updateQuestion");
        UpdateQuestionFunctionalitySagas saga = new UpdateQuestionFunctionalitySagas(
                unitOfWorkService, questionAggregateId, title, content, topicAggregateIds, unitOfWork,
                commandGateway);
        saga.executeWorkflow(unitOfWork);
    }

    public void deleteQuestion(Integer questionAggregateId) {
        SagaUnitOfWork unitOfWork = unitOfWorkService.createUnitOfWork("deleteQuestion");
        DeleteQuestionFunctionalitySagas saga = new DeleteQuestionFunctionalitySagas(
                unitOfWorkService, questionAggregateId, unitOfWork, commandGateway);
        saga.executeWorkflow(unitOfWork);
    }
}
