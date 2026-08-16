package pt.ulisboa.tecnico.socialsoftware.quizzesfull2.microservices.question.coordination.functionalities;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import pt.ulisboa.tecnico.socialsoftware.ms.messaging.CommandGateway;
import pt.ulisboa.tecnico.socialsoftware.ms.transaction.sagas.aggregate.GenericSagaState;
import pt.ulisboa.tecnico.socialsoftware.ms.transaction.sagas.aggregate.SagaAggregate;
import pt.ulisboa.tecnico.socialsoftware.ms.transaction.sagas.unitOfWork.SagaUnitOfWork;
import pt.ulisboa.tecnico.socialsoftware.ms.transaction.sagas.unitOfWork.SagaUnitOfWorkService;
import pt.ulisboa.tecnico.socialsoftware.quizzesfull2.microservices.question.aggregate.QuestionDto;
import pt.ulisboa.tecnico.socialsoftware.quizzesfull2.microservices.question.coordination.sagas.CreateQuestionFunctionalitySagas;
import pt.ulisboa.tecnico.socialsoftware.quizzesfull2.microservices.question.coordination.sagas.DeleteQuestionFunctionalitySagas;
import pt.ulisboa.tecnico.socialsoftware.quizzesfull2.microservices.question.coordination.sagas.GetQuestionByIdFunctionalitySagas;
import pt.ulisboa.tecnico.socialsoftware.quizzesfull2.microservices.question.coordination.sagas.GetQuestionsByCourseFunctionalitySagas;
import pt.ulisboa.tecnico.socialsoftware.quizzesfull2.microservices.question.coordination.sagas.UpdateQuestionFunctionalitySagas;
import pt.ulisboa.tecnico.socialsoftware.quizzesfull2.microservices.question.service.QuestionService;

import java.util.List;

@Service
public class QuestionFunctionalities {
    @Autowired
    private SagaUnitOfWorkService unitOfWorkService;
    @Autowired
    private CommandGateway commandGateway;
    @Autowired
    private QuestionService questionService;

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

    public void setTopicNameByEvent(Integer questionAggregateId, Integer topicAggregateId, String topicName,
                                    Long topicVersion) {
        SagaUnitOfWork unitOfWork = unitOfWorkService.createUnitOfWork("setTopicNameByEvent");
        if (isInSaga(questionAggregateId, unitOfWork)) {
            return;
        }
        questionService.setTopicName(questionAggregateId, topicAggregateId, topicName, topicVersion, unitOfWork);
        unitOfWorkService.commit(unitOfWork);
    }

    public void removeDeletedTopicByEvent(Integer questionAggregateId, Integer topicAggregateId) {
        SagaUnitOfWork unitOfWork = unitOfWorkService.createUnitOfWork("removeDeletedTopicByEvent");
        if (isInSaga(questionAggregateId, unitOfWork)) {
            return;
        }
        questionService.removeDeletedTopic(questionAggregateId, topicAggregateId, unitOfWork);
        unitOfWorkService.commit(unitOfWork);
    }

    // The guard belongs here rather than in the service methods, which the saga steps also call.
    private boolean isInSaga(Integer questionAggregateId, SagaUnitOfWork unitOfWork) {
        SagaAggregate question = (SagaAggregate) unitOfWorkService.aggregateLoadAndRegisterRead(
                questionAggregateId, unitOfWork);
        return !GenericSagaState.NOT_IN_SAGA.equals(question.getSagaState());
    }
}
