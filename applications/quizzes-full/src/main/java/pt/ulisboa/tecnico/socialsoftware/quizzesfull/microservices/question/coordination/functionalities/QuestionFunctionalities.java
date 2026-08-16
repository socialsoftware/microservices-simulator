package pt.ulisboa.tecnico.socialsoftware.quizzesfull.microservices.question.coordination.functionalities;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import pt.ulisboa.tecnico.socialsoftware.ms.messaging.CommandGateway;
import pt.ulisboa.tecnico.socialsoftware.ms.transaction.sagas.unitOfWork.SagaUnitOfWork;
import pt.ulisboa.tecnico.socialsoftware.ms.transaction.sagas.unitOfWork.SagaUnitOfWorkService;
import pt.ulisboa.tecnico.socialsoftware.quizzesfull.microservices.question.aggregate.Option;
import pt.ulisboa.tecnico.socialsoftware.quizzesfull.microservices.question.aggregate.QuestionDto;
import pt.ulisboa.tecnico.socialsoftware.quizzesfull.microservices.question.coordination.sagas.CreateQuestionFunctionalitySagas;
import pt.ulisboa.tecnico.socialsoftware.quizzesfull.microservices.question.coordination.sagas.DeleteQuestionFunctionalitySagas;
import pt.ulisboa.tecnico.socialsoftware.quizzesfull.microservices.question.coordination.sagas.GetQuestionByIdFunctionalitySagas;
import pt.ulisboa.tecnico.socialsoftware.quizzesfull.microservices.question.coordination.sagas.GetQuestionsByCourseExecutionIdFunctionalitySagas;
import pt.ulisboa.tecnico.socialsoftware.quizzesfull.microservices.question.coordination.sagas.UpdateQuestionFunctionalitySagas;
import pt.ulisboa.tecnico.socialsoftware.quizzesfull.microservices.question.service.QuestionService;

import java.util.List;
import java.util.Set;

@Service
public class QuestionFunctionalities {

    @Autowired
    private SagaUnitOfWorkService unitOfWorkService;

    @Autowired
    private CommandGateway commandGateway;

    @Autowired
    private QuestionService questionService;

    public QuestionDto createQuestion(String title, String content, Integer courseId,
                                      List<Integer> topicIds, Set<Option> options) {
        String functionalityName = new Throwable().getStackTrace()[0].getMethodName();
        SagaUnitOfWork unitOfWork = unitOfWorkService.createUnitOfWork(functionalityName);
        CreateQuestionFunctionalitySagas saga = new CreateQuestionFunctionalitySagas(
                unitOfWorkService, title, content, courseId, topicIds, options, unitOfWork, commandGateway);
        saga.executeWorkflow(unitOfWork);
        return saga.getCreatedQuestionDto();
    }

    public void updateQuestion(Integer questionId, String title, String content, List<Integer> topicIds) {
        String functionalityName = new Throwable().getStackTrace()[0].getMethodName();
        SagaUnitOfWork unitOfWork = unitOfWorkService.createUnitOfWork(functionalityName);
        UpdateQuestionFunctionalitySagas saga = new UpdateQuestionFunctionalitySagas(
                unitOfWorkService, questionId, title, content, topicIds, unitOfWork, commandGateway);
        saga.executeWorkflow(unitOfWork);
    }

    public void deleteQuestion(Integer questionId) {
        String functionalityName = new Throwable().getStackTrace()[0].getMethodName();
        SagaUnitOfWork unitOfWork = unitOfWorkService.createUnitOfWork(functionalityName);
        DeleteQuestionFunctionalitySagas saga = new DeleteQuestionFunctionalitySagas(
                unitOfWorkService, questionId, unitOfWork, commandGateway);
        saga.executeWorkflow(unitOfWork);
    }

    public QuestionDto getQuestionById(Integer questionId) {
        String functionalityName = new Throwable().getStackTrace()[0].getMethodName();
        SagaUnitOfWork unitOfWork = unitOfWorkService.createUnitOfWork(functionalityName);
        GetQuestionByIdFunctionalitySagas saga = new GetQuestionByIdFunctionalitySagas(
                unitOfWorkService, questionId, unitOfWork, commandGateway);
        saga.executeWorkflow(unitOfWork);
        return saga.getQuestionDto();
    }

    public List<QuestionDto> getQuestionsByCourseExecutionId(Integer executionId) {
        String functionalityName = new Throwable().getStackTrace()[0].getMethodName();
        SagaUnitOfWork unitOfWork = unitOfWorkService.createUnitOfWork(functionalityName);
        GetQuestionsByCourseExecutionIdFunctionalitySagas saga = new GetQuestionsByCourseExecutionIdFunctionalitySagas(
                unitOfWorkService, executionId, unitOfWork, commandGateway);
        saga.executeWorkflow(unitOfWork);
        return saga.getQuestions();
    }

    public void updateTopicNameInQuestionByEvent(Integer questionId, Integer topicId, String topicName) {
        SagaUnitOfWork unitOfWork = unitOfWorkService.createUnitOfWork("updateTopicNameInQuestionByEvent");
        questionService.updateTopicNameInQuestion(questionId, topicId, topicName, unitOfWork);
        unitOfWorkService.commit(unitOfWork);
    }

    public void removeTopicFromQuestionByEvent(Integer questionId, Integer topicId) {
        SagaUnitOfWork unitOfWork = unitOfWorkService.createUnitOfWork("removeTopicFromQuestionByEvent");
        questionService.removeTopicFromQuestion(questionId, topicId, unitOfWork);
        unitOfWorkService.commit(unitOfWork);
    }
}
