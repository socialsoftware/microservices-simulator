package pt.ulisboa.tecnico.socialsoftware.answers.coordination.functionalities;

import static pt.ulisboa.tecnico.socialsoftware.ms.TransactionalModel.SAGAS;
import static pt.ulisboa.tecnico.socialsoftware.answers.microservices.exception.AnswersErrorMessage.*;

import java.util.Arrays;
import pt.ulisboa.tecnico.socialsoftware.answers.microservices.exception.AnswersException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Service;
import jakarta.annotation.PostConstruct;
import pt.ulisboa.tecnico.socialsoftware.ms.TransactionalModel;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.unitOfWork.SagaUnitOfWork;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.unitOfWork.SagaUnitOfWorkService;
import pt.ulisboa.tecnico.socialsoftware.answers.sagas.coordination.question.*;
import pt.ulisboa.tecnico.socialsoftware.answers.microservices.question.service.QuestionService;
import pt.ulisboa.tecnico.socialsoftware.answers.shared.dtos.QuestionDto;
import java.util.List;

@Service
public class QuestionFunctionalities {
    @Autowired
    private QuestionService questionService;

    @Autowired
    private SagaUnitOfWorkService sagaUnitOfWorkService;


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
                        questionService, sagaUnitOfWorkService, aggregateId, sagaUnitOfWork);
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
                        questionService, sagaUnitOfWorkService, courseAggregateId, sagaUnitOfWork);
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
                        questionService, sagaUnitOfWorkService, courseAggregateId, questionDto, sagaUnitOfWork);
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
                        questionService, sagaUnitOfWorkService, questionDto, sagaUnitOfWork);
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
                        questionService, sagaUnitOfWorkService, questionAggregateId, sagaUnitOfWork);
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
                        questionService, sagaUnitOfWorkService, courseAggregateId, topicIds, sagaUnitOfWork);
                updateQuestionTopicsFunctionalitySagas.executeWorkflow(sagaUnitOfWork);
                break;
            default: throw new AnswersException(UNDEFINED_TRANSACTIONAL_MODEL);
        }
    }

        private void checkInput(QuestionDto questionDto) {
        if (questionDto.getTitle() == null) {
            throw new AnswersException(QUESTION_MISSING_TITLE);
        }
        if (questionDto.getContent() == null) {
            throw new AnswersException(QUESTION_MISSING_CONTENT);
        }
    }
}