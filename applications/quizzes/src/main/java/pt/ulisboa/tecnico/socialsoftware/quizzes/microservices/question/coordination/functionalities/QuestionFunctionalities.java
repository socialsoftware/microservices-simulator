package pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.question.coordination.functionalities;

import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Service;
import pt.ulisboa.tecnico.socialsoftware.ms.TransactionalModel;
import pt.ulisboa.tecnico.socialsoftware.ms.causal.unitOfWork.CausalUnitOfWork;
import pt.ulisboa.tecnico.socialsoftware.ms.causal.unitOfWork.CausalUnitOfWorkService;
import pt.ulisboa.tecnico.socialsoftware.ms.coordination.workflow.command.CommandGateway;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.unitOfWork.SagaUnitOfWork;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.unitOfWork.SagaUnitOfWorkService;
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.exception.QuizzesException;
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.question.aggregate.QuestionDto;
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.question.coordination.causal.*;
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.question.coordination.sagas.*;

import java.util.Arrays;
import java.util.List;

import static pt.ulisboa.tecnico.socialsoftware.ms.TransactionalModel.SAGAS;
import static pt.ulisboa.tecnico.socialsoftware.ms.TransactionalModel.TCC;
import static pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.exception.QuizzesErrorMessage.UNDEFINED_TRANSACTIONAL_MODEL;

@Service
public class QuestionFunctionalities {
    @Autowired(required = false)
    private SagaUnitOfWorkService sagaUnitOfWorkService;
    @Autowired(required = false)
    private CausalUnitOfWorkService causalUnitOfWorkService;

    @Autowired
    private Environment env;
    @Autowired
    private CommandGateway commandGateway;

    private TransactionalModel workflowType;

    @PostConstruct
    public void init() {
        String[] activeProfiles = env.getActiveProfiles();
        if (Arrays.asList(activeProfiles).contains(SAGAS.getValue())) {
            workflowType = SAGAS;
        } else if (Arrays.asList(activeProfiles).contains(TCC.getValue())) {
            workflowType = TCC;
        } else {
            throw new QuizzesException(UNDEFINED_TRANSACTIONAL_MODEL);
        }
    }

    public QuestionDto findQuestionByAggregateId(Integer aggregateId) {
        String functionalityName = new Throwable().getStackTrace()[0].getMethodName();

        switch (workflowType) {
            case SAGAS:
                SagaUnitOfWork sagaUnitOfWork = sagaUnitOfWorkService.createUnitOfWork(functionalityName);
                FindQuestionByAggregateIdFunctionalitySagas findQuestionByAggregateIdFunctionalitySagas = new FindQuestionByAggregateIdFunctionalitySagas(
                        sagaUnitOfWorkService, aggregateId, sagaUnitOfWork, commandGateway);
                findQuestionByAggregateIdFunctionalitySagas.executeWorkflow(sagaUnitOfWork);
                return findQuestionByAggregateIdFunctionalitySagas.getQuestionDto();
            case TCC:
                CausalUnitOfWork causalUnitOfWork = causalUnitOfWorkService.createUnitOfWork(functionalityName);
                FindQuestionByAggregateIdFunctionalityTCC findQuestionByAggregateIdFunctionalityTCC = new FindQuestionByAggregateIdFunctionalityTCC(
                        causalUnitOfWorkService, aggregateId, causalUnitOfWork, commandGateway);
                findQuestionByAggregateIdFunctionalityTCC.executeWorkflow(causalUnitOfWork);
                return findQuestionByAggregateIdFunctionalityTCC.getQuestionDto();
            default:
                throw new QuizzesException(UNDEFINED_TRANSACTIONAL_MODEL);
        }
    }

    public List<QuestionDto> findQuestionsByCourseAggregateId(Integer courseAggregateId) {
        String functionalityName = new Throwable().getStackTrace()[0].getMethodName();

        switch (workflowType) {
            case SAGAS:
                SagaUnitOfWork sagaUnitOfWork = sagaUnitOfWorkService.createUnitOfWork(functionalityName);
                FindQuestionsByCourseFunctionalitySagas findQuestionsByCourseFunctionalitySagas = new FindQuestionsByCourseFunctionalitySagas(
                        sagaUnitOfWorkService, courseAggregateId, sagaUnitOfWork, commandGateway);
                findQuestionsByCourseFunctionalitySagas.executeWorkflow(sagaUnitOfWork);
                return findQuestionsByCourseFunctionalitySagas.getQuestions();
            case TCC:
                CausalUnitOfWork causalUnitOfWork = causalUnitOfWorkService.createUnitOfWork(functionalityName);
                FindQuestionsByCourseFunctionalityTCC findQuestionsByCourseFunctionalityTCC = new FindQuestionsByCourseFunctionalityTCC(
                        causalUnitOfWorkService, courseAggregateId, causalUnitOfWork, commandGateway);
                findQuestionsByCourseFunctionalityTCC.executeWorkflow(causalUnitOfWork);
                return findQuestionsByCourseFunctionalityTCC.getQuestions();
            default:
                throw new QuizzesException(UNDEFINED_TRANSACTIONAL_MODEL);
        }
    }

    public QuestionDto createQuestion(Integer courseAggregateId, QuestionDto questionDto) throws QuizzesException {
        String functionalityName = new Throwable().getStackTrace()[0].getMethodName();

        switch (workflowType) {
            case SAGAS:
                SagaUnitOfWork sagaUnitOfWork = sagaUnitOfWorkService.createUnitOfWork(functionalityName);
                CreateQuestionFunctionalitySagas createQuestionFunctionalitySagas = new CreateQuestionFunctionalitySagas(
                        sagaUnitOfWorkService, courseAggregateId,
                        questionDto, sagaUnitOfWork, commandGateway);
                createQuestionFunctionalitySagas.executeWorkflow(sagaUnitOfWork);
                return createQuestionFunctionalitySagas.getCreatedQuestion();
            case TCC:
                CausalUnitOfWork causalUnitOfWork = causalUnitOfWorkService.createUnitOfWork(functionalityName);
                CreateQuestionFunctionalityTCC createQuestionFunctionalityTCC = new CreateQuestionFunctionalityTCC(
                        causalUnitOfWorkService, courseAggregateId,
                        questionDto, causalUnitOfWork, commandGateway);
                createQuestionFunctionalityTCC.executeWorkflow(causalUnitOfWork);
                return createQuestionFunctionalityTCC.getCreatedQuestion();
            default:
                throw new QuizzesException(UNDEFINED_TRANSACTIONAL_MODEL);
        }
    }

    public void updateQuestion(QuestionDto questionDto) {
        String functionalityName = new Throwable().getStackTrace()[0].getMethodName();

        switch (workflowType) {
            case SAGAS:
                SagaUnitOfWork sagaUnitOfWork = sagaUnitOfWorkService.createUnitOfWork(functionalityName);
                UpdateQuestionFunctionalitySagas updateQuestionFunctionalitySagas = new UpdateQuestionFunctionalitySagas(
                        sagaUnitOfWorkService, questionDto, sagaUnitOfWork,
                        commandGateway);
                updateQuestionFunctionalitySagas.executeWorkflow(sagaUnitOfWork);
                break;
            case TCC:
                CausalUnitOfWork causalUnitOfWork = causalUnitOfWorkService.createUnitOfWork(functionalityName);
                UpdateQuestionFunctionalityTCC updateQuestionFunctionalityTCC = new UpdateQuestionFunctionalityTCC(
                        causalUnitOfWorkService, questionDto, causalUnitOfWork,
                        commandGateway);
                updateQuestionFunctionalityTCC.executeWorkflow(causalUnitOfWork);
                break;
            default:
                throw new QuizzesException(UNDEFINED_TRANSACTIONAL_MODEL);
        }
    }

    public void removeQuestion(Integer questionAggregateId) {
        String functionalityName = new Throwable().getStackTrace()[0].getMethodName();

        switch (workflowType) {
            case SAGAS:
                SagaUnitOfWork sagaUnitOfWork = sagaUnitOfWorkService.createUnitOfWork(functionalityName);
                RemoveQuestionFunctionalitySagas removeQuestionFunctionalitySagas = new RemoveQuestionFunctionalitySagas(
                        sagaUnitOfWorkService, questionAggregateId, sagaUnitOfWork, commandGateway);
                removeQuestionFunctionalitySagas.executeWorkflow(sagaUnitOfWork);
                break;
            case TCC:
                CausalUnitOfWork causalUnitOfWork = causalUnitOfWorkService.createUnitOfWork(functionalityName);
                RemoveQuestionFunctionalityTCC removeQuestionFunctionalityTCC = new RemoveQuestionFunctionalityTCC(
                        causalUnitOfWorkService, questionAggregateId, causalUnitOfWork,
                        commandGateway);
                removeQuestionFunctionalityTCC.executeWorkflow(causalUnitOfWork);
                break;
            default:
                throw new QuizzesException(UNDEFINED_TRANSACTIONAL_MODEL);
        }
    }

    public void updateQuestionTopics(Integer courseAggregateId, List<Integer> topicIds) {
        String functionalityName = new Throwable().getStackTrace()[0].getMethodName();

        switch (workflowType) {
            case SAGAS:
                SagaUnitOfWork sagaUnitOfWork = sagaUnitOfWorkService.createUnitOfWork(functionalityName);
                UpdateQuestionTopicsFunctionalitySagas updateQuestionTopicsFunctionalitySagas = new UpdateQuestionTopicsFunctionalitySagas(
                        sagaUnitOfWorkService, courseAggregateId,
                        topicIds, sagaUnitOfWork, commandGateway);
                updateQuestionTopicsFunctionalitySagas.executeWorkflow(sagaUnitOfWork);
                break;
            case TCC:
                CausalUnitOfWork causalUnitOfWork = causalUnitOfWorkService.createUnitOfWork(functionalityName);
                UpdateQuestionTopicsFunctionalityTCC updateQuestionTopicsFunctionalityTCC = new UpdateQuestionTopicsFunctionalityTCC(
                        causalUnitOfWorkService, courseAggregateId,
                        topicIds, causalUnitOfWork, commandGateway);
                updateQuestionTopicsFunctionalityTCC.executeWorkflow(causalUnitOfWork);
                break;
            default:
                throw new QuizzesException(UNDEFINED_TRANSACTIONAL_MODEL);
        }
    }

    public void updateTopicInQuestion(Integer questionAggregateId, Integer aggregateTopicId, String topicName,
            Integer eventVersion) {
        String functionalityName = new Throwable().getStackTrace()[0].getMethodName();
        switch (workflowType) {
            case SAGAS:
                SagaUnitOfWork sagaUnitOfWork = sagaUnitOfWorkService.createUnitOfWork(functionalityName);
                UpdateTopicInQuestionFunctionalitySagas functionalitySagas = new UpdateTopicInQuestionFunctionalitySagas(
                        sagaUnitOfWorkService, questionAggregateId, aggregateTopicId, topicName, eventVersion,
                        sagaUnitOfWork, commandGateway);
                functionalitySagas.executeWorkflow(sagaUnitOfWork);
                break;
            case TCC:
                CausalUnitOfWork causalUnitOfWork = causalUnitOfWorkService.createUnitOfWork(functionalityName);
                UpdateTopicInQuestionFunctionalityTCC functionalityTCC = new UpdateTopicInQuestionFunctionalityTCC(
                        causalUnitOfWorkService, questionAggregateId, aggregateTopicId, topicName, eventVersion,
                        causalUnitOfWork, commandGateway);
                functionalityTCC.executeWorkflow(causalUnitOfWork);
                break;
            default:
                throw new QuizzesException(UNDEFINED_TRANSACTIONAL_MODEL);
        }
    }

    public void deleteTopicInQuestion(Integer questionAggregateId, Integer aggregateTopicId, Integer eventVersion) {
        String functionalityName = new Throwable().getStackTrace()[0].getMethodName();
        switch (workflowType) {
            case SAGAS:
                SagaUnitOfWork sagaUnitOfWork = sagaUnitOfWorkService.createUnitOfWork(functionalityName);
                DeleteTopicInQuestionFunctionalitySagas functionalitySagas = new DeleteTopicInQuestionFunctionalitySagas(
                        sagaUnitOfWorkService, questionAggregateId, aggregateTopicId, eventVersion, sagaUnitOfWork,
                        commandGateway);
                functionalitySagas.executeWorkflow(sagaUnitOfWork);
                break;
            case TCC:
                CausalUnitOfWork causalUnitOfWork = causalUnitOfWorkService.createUnitOfWork(functionalityName);
                DeleteTopicInQuestionFunctionalityTCC functionalityTCC = new DeleteTopicInQuestionFunctionalityTCC(
                        causalUnitOfWorkService, questionAggregateId, aggregateTopicId, eventVersion, causalUnitOfWork,
                        commandGateway);
                functionalityTCC.executeWorkflow(causalUnitOfWork);
                break;
            default:
                throw new QuizzesException(UNDEFINED_TRANSACTIONAL_MODEL);
        }
    }

}
