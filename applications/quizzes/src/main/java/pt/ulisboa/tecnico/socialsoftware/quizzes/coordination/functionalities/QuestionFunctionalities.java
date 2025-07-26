package pt.ulisboa.tecnico.socialsoftware.quizzes.coordination.functionalities;

import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Service;
import pt.ulisboa.tecnico.socialsoftware.ms.TransactionalModel;
import pt.ulisboa.tecnico.socialsoftware.ms.causal.unitOfWork.CausalUnitOfWork;
import pt.ulisboa.tecnico.socialsoftware.ms.causal.unitOfWork.CausalUnitOfWorkService;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.unitOfWork.SagaUnitOfWork;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.unitOfWork.SagaUnitOfWorkService;
import pt.ulisboa.tecnico.socialsoftware.quizzes.causal.coordination.question.*;
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.course.service.CourseService;
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.exception.QuizzesException;
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.question.aggregate.QuestionDto;
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.question.aggregate.QuestionFactory;
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.question.service.QuestionService;
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.topic.service.TopicService;
import pt.ulisboa.tecnico.socialsoftware.quizzes.sagas.coordination.question.*;

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
                        questionService, sagaUnitOfWorkService, aggregateId, sagaUnitOfWork);
                findQuestionByAggregateIdFunctionalitySagas.executeWorkflow(sagaUnitOfWork);
                return findQuestionByAggregateIdFunctionalitySagas.getQuestionDto();
            case TCC:
                CausalUnitOfWork causalUnitOfWork = causalUnitOfWorkService.createUnitOfWork(functionalityName);
                FindQuestionByAggregateIdFunctionalityTCC findQuestionByAggregateIdFunctionalityTCC = new FindQuestionByAggregateIdFunctionalityTCC(
                        questionService, causalUnitOfWorkService, aggregateId, causalUnitOfWork);
                findQuestionByAggregateIdFunctionalityTCC.executeWorkflow(causalUnitOfWork);
                return findQuestionByAggregateIdFunctionalityTCC.getQuestionDto();
            default: throw new QuizzesException(UNDEFINED_TRANSACTIONAL_MODEL);
        }
    }

    public List<QuestionDto> findQuestionsByCourseAggregateId(Integer courseAggregateId) {
        String functionalityName = new Throwable().getStackTrace()[0].getMethodName();

        switch (workflowType) {
            case SAGAS:
                SagaUnitOfWork sagaUnitOfWork = sagaUnitOfWorkService.createUnitOfWork(functionalityName);
                FindQuestionsByCourseFunctionalitySagas findQuestionsByCourseFunctionalitySagas = new FindQuestionsByCourseFunctionalitySagas(
                        questionService, sagaUnitOfWorkService, courseAggregateId, sagaUnitOfWork);
                findQuestionsByCourseFunctionalitySagas.executeWorkflow(sagaUnitOfWork);
                return findQuestionsByCourseFunctionalitySagas.getQuestions();
            case TCC:
                CausalUnitOfWork causalUnitOfWork = causalUnitOfWorkService.createUnitOfWork(functionalityName);
                FindQuestionsByCourseFunctionalityTCC findQuestionsByCourseFunctionalityTCC = new FindQuestionsByCourseFunctionalityTCC(
                        questionService, causalUnitOfWorkService, courseAggregateId, causalUnitOfWork);
                findQuestionsByCourseFunctionalityTCC.executeWorkflow(causalUnitOfWork);
                return findQuestionsByCourseFunctionalityTCC.getQuestions();
            default: throw new QuizzesException(UNDEFINED_TRANSACTIONAL_MODEL);
        }
    }

    public QuestionDto createQuestion(Integer courseAggregateId, QuestionDto questionDto) throws QuizzesException {
        String functionalityName = new Throwable().getStackTrace()[0].getMethodName();

        switch (workflowType) {
            case SAGAS:
                SagaUnitOfWork sagaUnitOfWork = sagaUnitOfWorkService.createUnitOfWork(functionalityName);
                CreateQuestionFunctionalitySagas createQuestionFunctionalitySagas = new CreateQuestionFunctionalitySagas(
                        questionService, topicService, courseService, sagaUnitOfWorkService, courseAggregateId, questionDto, sagaUnitOfWork);
                createQuestionFunctionalitySagas.executeWorkflow(sagaUnitOfWork);
                return createQuestionFunctionalitySagas.getCreatedQuestion();
            case TCC:
                CausalUnitOfWork causalUnitOfWork = causalUnitOfWorkService.createUnitOfWork(functionalityName);
                CreateQuestionFunctionalityTCC createQuestionFunctionalityTCC = new CreateQuestionFunctionalityTCC(
                        questionService, topicService, courseService, causalUnitOfWorkService, courseAggregateId, questionDto, causalUnitOfWork);
                createQuestionFunctionalityTCC.executeWorkflow(causalUnitOfWork);
                return createQuestionFunctionalityTCC.getCreatedQuestion();
            default: throw new QuizzesException(UNDEFINED_TRANSACTIONAL_MODEL);
        }
    }

    public void updateQuestion(QuestionDto questionDto) {
        String functionalityName = new Throwable().getStackTrace()[0].getMethodName();

        switch (workflowType) {
            case SAGAS:
                SagaUnitOfWork sagaUnitOfWork = sagaUnitOfWorkService.createUnitOfWork(functionalityName);
                UpdateQuestionFunctionalitySagas updateQuestionFunctionalitySagas = new UpdateQuestionFunctionalitySagas(
                        questionService, sagaUnitOfWorkService, questionFactory, questionDto, sagaUnitOfWork);
                updateQuestionFunctionalitySagas.executeWorkflow(sagaUnitOfWork);
                break;
            case TCC:
                CausalUnitOfWork causalUnitOfWork = causalUnitOfWorkService.createUnitOfWork(functionalityName);
                UpdateQuestionFunctionalityTCC updateQuestionFunctionalityTCC = new UpdateQuestionFunctionalityTCC(
                        questionService, causalUnitOfWorkService, questionFactory, questionDto, causalUnitOfWork);
                updateQuestionFunctionalityTCC.executeWorkflow(causalUnitOfWork);
                break;
            default: throw new QuizzesException(UNDEFINED_TRANSACTIONAL_MODEL);
        }
    }

    public void removeQuestion(Integer questionAggregateId) {
        String functionalityName = new Throwable().getStackTrace()[0].getMethodName();

        switch (workflowType) {
            case SAGAS:
                SagaUnitOfWork sagaUnitOfWork = sagaUnitOfWorkService.createUnitOfWork(functionalityName);
                RemoveQuestionFunctionalitySagas removeQuestionFunctionalitySagas = new RemoveQuestionFunctionalitySagas(
                        questionService, sagaUnitOfWorkService, questionAggregateId, sagaUnitOfWork);
                removeQuestionFunctionalitySagas.executeWorkflow(sagaUnitOfWork);
                break;
            case TCC:
                CausalUnitOfWork causalUnitOfWork = causalUnitOfWorkService.createUnitOfWork(functionalityName);
                RemoveQuestionFunctionalityTCC removeQuestionFunctionalityTCC = new RemoveQuestionFunctionalityTCC(
                        questionService, causalUnitOfWorkService, questionAggregateId, causalUnitOfWork);
                removeQuestionFunctionalityTCC.executeWorkflow(causalUnitOfWork);
                break;
            default: throw new QuizzesException(UNDEFINED_TRANSACTIONAL_MODEL);
        }
    }

    public void updateQuestionTopics(Integer courseAggregateId, List<Integer> topicIds) {
        String functionalityName = new Throwable().getStackTrace()[0].getMethodName();

        switch (workflowType) {
            case SAGAS:
                SagaUnitOfWork sagaUnitOfWork = sagaUnitOfWorkService.createUnitOfWork(functionalityName);
                UpdateQuestionTopicsFunctionalitySagas updateQuestionTopicsFunctionalitySagas = new UpdateQuestionTopicsFunctionalitySagas(
                        questionService, topicService, questionFactory, sagaUnitOfWorkService, courseAggregateId, topicIds, sagaUnitOfWork);
                updateQuestionTopicsFunctionalitySagas.executeWorkflow(sagaUnitOfWork);
                break;
            case TCC:
                CausalUnitOfWork causalUnitOfWork = causalUnitOfWorkService.createUnitOfWork(functionalityName);
                UpdateQuestionTopicsFunctionalityTCC updateQuestionTopicsFunctionalityTCC = new UpdateQuestionTopicsFunctionalityTCC(
                        questionService, topicService, questionFactory, causalUnitOfWorkService, courseAggregateId, topicIds, causalUnitOfWork);
                updateQuestionTopicsFunctionalityTCC.executeWorkflow(causalUnitOfWork);
                break;
            default: throw new QuizzesException(UNDEFINED_TRANSACTIONAL_MODEL);
        }
    }

}
