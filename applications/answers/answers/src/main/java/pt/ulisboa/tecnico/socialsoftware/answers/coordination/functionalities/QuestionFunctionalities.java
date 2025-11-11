package pt.ulisboa.tecnico.socialsoftware.answers.coordination.functionalities;

import static pt.ulisboa.tecnico.socialsoftware.ms.TransactionalModel.SAGAS;
import static pt.ulisboa.tecnico.socialsoftware.answers.microservices.exception.AnswersErrorMessage.*;

import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.ArrayList;
import java.util.HashSet;
import pt.ulisboa.tecnico.socialsoftware.answers.microservices.exception.AnswersException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Service;
import jakarta.annotation.PostConstruct;
import pt.ulisboa.tecnico.socialsoftware.ms.TransactionalModel;
import pt.ulisboa.tecnico.socialsoftware.ms.coordination.unitOfWork.UnitOfWork;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.unitOfWork.SagaUnitOfWork;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.unitOfWork.SagaUnitOfWorkService;
import pt.ulisboa.tecnico.socialsoftware.answers.sagas.coordination.question.*;
import pt.ulisboa.tecnico.socialsoftware.ms.domain.aggregate.Aggregate.AggregateState;
import pt.ulisboa.tecnico.socialsoftware.answers.microservices.question.service.QuestionService;
import pt.ulisboa.tecnico.socialsoftware.answers.microservices.questionfactory.service.QuestionFactory;
import pt.ulisboa.tecnico.socialsoftware.answers.microservices.question.aggregate.QuestionDto;
import pt.ulisboa.tecnico.socialsoftware.answers.microservices.question.aggregate.QuestionCourseDto;
import pt.ulisboa.tecnico.socialsoftware.answers.microservices.question.aggregate.QuestionTopicDto;
import pt.ulisboa.tecnico.socialsoftware.answers.microservices.question.aggregate.OptionDto;

@Service
public class QuestionFunctionalities {
    @Autowired
    private QuestionService questionService;

    @Autowired
    private SagaUnitOfWorkService sagaUnitOfWorkService;

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
        } else {
            throw new AnswersException(UNDEFINED_TRANSACTIONAL_MODEL);
        }
    }

    public QuestionDto findQuestionByAggregateId(Integer aggregateId) {
        String functionalityName = new Throwable().getStackTrace()[0].getMethodName();

        switch (workflowType) {
            case SAGAS:
                SagaUnitOfWork sagaUnitOfWork = sagaUnitOfWorkService.createUnitOfWork(functionalityName);
                FindQuestionByAggregateIdFunctionalitySagas findQuestionByAggregateIdFunctionalitySagas = new FindQuestionByAggregateIdFunctionalitySagas(
                        questionService, sagaUnitOfWorkService, sagaUnitOfWork);
                findQuestionByAggregateIdFunctionalitySagas.executeWorkflow(sagaUnitOfWork);
                return findQuestionByAggregateIdFunctionalitySagas.getResult();
            default: throw new AnswersException(UNDEFINED_TRANSACTIONAL_MODEL);
        }
    }

    public List<QuestionDto> findQuestionsByCourseAggregateId(Integer courseAggregateId) {
        String functionalityName = new Throwable().getStackTrace()[0].getMethodName();

        switch (workflowType) {
            case SAGAS:
                SagaUnitOfWork sagaUnitOfWork = sagaUnitOfWorkService.createUnitOfWork(functionalityName);
                FindQuestionsByCourseAggregateIdFunctionalitySagas findQuestionsByCourseAggregateIdFunctionalitySagas = new FindQuestionsByCourseAggregateIdFunctionalitySagas(
                        questionService, sagaUnitOfWorkService, sagaUnitOfWork);
                findQuestionsByCourseAggregateIdFunctionalitySagas.executeWorkflow(sagaUnitOfWork);
                return findQuestionsByCourseAggregateIdFunctionalitySagas.getFindQuestionsByCourseAggregateId();
            default: throw new AnswersException(UNDEFINED_TRANSACTIONAL_MODEL);
        }
    }

    public QuestionDto createQuestion(Integer courseAggregateId, QuestionDto questionDto) throws AnswersException {
        String functionalityName = new Throwable().getStackTrace()[0].getMethodName();

        switch (workflowType) {
            case SAGAS:
                SagaUnitOfWork sagaUnitOfWork = sagaUnitOfWorkService.createUnitOfWork(functionalityName);
                CreateQuestionFunctionalitySagas createQuestionFunctionalitySagas = new CreateQuestionFunctionalitySagas(
                        questionService, sagaUnitOfWorkService, sagaUnitOfWork);
                createQuestionFunctionalitySagas.executeWorkflow(sagaUnitOfWork);
                return createQuestionFunctionalitySagas.getCreatedQuestion();
            default: throw new AnswersException(UNDEFINED_TRANSACTIONAL_MODEL);
        }
    }

    public void updateQuestion(QuestionDto questionDto) throws AnswersException {
        String functionalityName = new Throwable().getStackTrace()[0].getMethodName();

        switch (workflowType) {
            case SAGAS:
                SagaUnitOfWork sagaUnitOfWork = sagaUnitOfWorkService.createUnitOfWork(functionalityName);
                UpdateQuestionFunctionalitySagas updateQuestionFunctionalitySagas = new UpdateQuestionFunctionalitySagas(
                        questionService, sagaUnitOfWorkService, sagaUnitOfWork);
                updateQuestionFunctionalitySagas.executeWorkflow(sagaUnitOfWork);
                break;
            default: throw new AnswersException(UNDEFINED_TRANSACTIONAL_MODEL);
        }
    }

    public void removeQuestion(Integer questionAggregateId) throws AnswersException {
        String functionalityName = new Throwable().getStackTrace()[0].getMethodName();

        switch (workflowType) {
            case SAGAS:
                SagaUnitOfWork sagaUnitOfWork = sagaUnitOfWorkService.createUnitOfWork(functionalityName);
                RemoveQuestionFunctionalitySagas removeQuestionFunctionalitySagas = new RemoveQuestionFunctionalitySagas(
                        questionService, sagaUnitOfWorkService, sagaUnitOfWork);
                removeQuestionFunctionalitySagas.executeWorkflow(sagaUnitOfWork);
                break;
            default: throw new AnswersException(UNDEFINED_TRANSACTIONAL_MODEL);
        }
    }

    public void updateQuestionTopics(Integer courseAggregateId, String topicIds) throws AnswersException {
        String functionalityName = new Throwable().getStackTrace()[0].getMethodName();

        switch (workflowType) {
            case SAGAS:
                SagaUnitOfWork sagaUnitOfWork = sagaUnitOfWorkService.createUnitOfWork(functionalityName);
                UpdateQuestionTopicsFunctionalitySagas updateQuestionTopicsFunctionalitySagas = new UpdateQuestionTopicsFunctionalitySagas(
                        questionService, sagaUnitOfWorkService, sagaUnitOfWork);
                updateQuestionTopicsFunctionalitySagas.executeWorkflow(sagaUnitOfWork);
                break;
            default: throw new AnswersException(UNDEFINED_TRANSACTIONAL_MODEL);
        }
    }

}