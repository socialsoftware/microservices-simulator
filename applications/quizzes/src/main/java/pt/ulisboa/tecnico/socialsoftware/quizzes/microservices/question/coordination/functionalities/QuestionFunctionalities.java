package pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.question.coordination.functionalities;

import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Service;
import pt.ulisboa.tecnico.socialsoftware.ms.TransactionalModel;
import pt.ulisboa.tecnico.socialsoftware.ms.causal.unitOfWork.CausalUnitOfWork;
import pt.ulisboa.tecnico.socialsoftware.ms.causal.unitOfWork.CausalUnitOfWorkService;
import pt.ulisboa.tecnico.socialsoftware.ms.coordination.workflow.CommandGateway;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.unitOfWork.SagaUnitOfWork;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.unitOfWork.SagaUnitOfWorkService;
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.course.service.CourseService;
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.exception.QuizzesException;
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.question.aggregate.QuestionDto;
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.question.aggregate.QuestionFactory;
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.question.coordination.causal.*;
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.question.coordination.sagas.*;
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.question.service.QuestionService;
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.topic.service.TopicService;

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
    private QuestionService questionService;
    @Autowired
    private CourseService courseService;
    @Autowired
    private TopicService topicService;
    @Autowired
    private QuestionFactory questionFactory;

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
                        sagaUnitOfWorkService, questionFactory, questionDto, sagaUnitOfWork,
                        commandGateway);
                updateQuestionFunctionalitySagas.executeWorkflow(sagaUnitOfWork);
                break;
            case TCC:
                CausalUnitOfWork causalUnitOfWork = causalUnitOfWorkService.createUnitOfWork(functionalityName);
                UpdateQuestionFunctionalityTCC updateQuestionFunctionalityTCC = new UpdateQuestionFunctionalityTCC(
                        causalUnitOfWorkService, questionFactory, questionDto, causalUnitOfWork,
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
                        questionFactory, sagaUnitOfWorkService, courseAggregateId,
                        topicIds, sagaUnitOfWork, commandGateway);
                updateQuestionTopicsFunctionalitySagas.executeWorkflow(sagaUnitOfWork);
                break;
            case TCC:
                CausalUnitOfWork causalUnitOfWork = causalUnitOfWorkService.createUnitOfWork(functionalityName);
                UpdateQuestionTopicsFunctionalityTCC updateQuestionTopicsFunctionalityTCC = new UpdateQuestionTopicsFunctionalityTCC(
                        questionFactory, causalUnitOfWorkService, courseAggregateId,
                        topicIds, causalUnitOfWork, commandGateway);
                updateQuestionTopicsFunctionalityTCC.executeWorkflow(causalUnitOfWork);
                break;
            default:
                throw new QuizzesException(UNDEFINED_TRANSACTIONAL_MODEL);
        }
    }

}
