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
import pt.ulisboa.tecnico.socialsoftware.answers.microservices.course.service.CourseService;
import pt.ulisboa.tecnico.socialsoftware.answers.microservices.topic.service.TopicService;
import pt.ulisboa.tecnico.socialsoftware.answers.shared.dtos.QuestionDto;
import java.util.List;

@Service
public class QuestionFunctionalities {
    @Autowired
    private QuestionService questionService;

    @Autowired
    private CourseService courseService;

    @Autowired
    private TopicService topicService;

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

    public QuestionDto createQuestion(QuestionDto questionDto) {
        String functionalityName = new Throwable().getStackTrace()[0].getMethodName();

        switch (workflowType) {
            case SAGAS:
                SagaUnitOfWork sagaUnitOfWork = sagaUnitOfWorkService.createUnitOfWork(functionalityName);
                checkInput(questionDto);
                CreateQuestionFunctionalitySagas createQuestionFunctionalitySagas = new CreateQuestionFunctionalitySagas(
                        sagaUnitOfWork, sagaUnitOfWorkService, questionService, courseService, topicService, questionDto);
                createQuestionFunctionalitySagas.executeWorkflow(sagaUnitOfWork);
                return createQuestionFunctionalitySagas.getCreatedQuestionDto();
            default: throw new AnswersException(UNDEFINED_TRANSACTIONAL_MODEL);
        }
    }

    public QuestionDto getQuestionById(Integer questionAggregateId) {
        String functionalityName = new Throwable().getStackTrace()[0].getMethodName();

        switch (workflowType) {
            case SAGAS:
                SagaUnitOfWork sagaUnitOfWork = sagaUnitOfWorkService.createUnitOfWork(functionalityName);
                GetQuestionByIdFunctionalitySagas getQuestionByIdFunctionalitySagas = new GetQuestionByIdFunctionalitySagas(
                        sagaUnitOfWork, sagaUnitOfWorkService, questionService, questionAggregateId);
                getQuestionByIdFunctionalitySagas.executeWorkflow(sagaUnitOfWork);
                return getQuestionByIdFunctionalitySagas.getQuestionDto();
            default: throw new AnswersException(UNDEFINED_TRANSACTIONAL_MODEL);
        }
    }

    public QuestionDto updateQuestion(QuestionDto questionDto) {
        String functionalityName = new Throwable().getStackTrace()[0].getMethodName();

        switch (workflowType) {
            case SAGAS:
                SagaUnitOfWork sagaUnitOfWork = sagaUnitOfWorkService.createUnitOfWork(functionalityName);
                checkInput(questionDto);
                UpdateQuestionFunctionalitySagas updateQuestionFunctionalitySagas = new UpdateQuestionFunctionalitySagas(
                        sagaUnitOfWork, sagaUnitOfWorkService, questionService, questionDto);
                updateQuestionFunctionalitySagas.executeWorkflow(sagaUnitOfWork);
                return updateQuestionFunctionalitySagas.getUpdatedQuestionDto();
            default: throw new AnswersException(UNDEFINED_TRANSACTIONAL_MODEL);
        }
    }

    public void deleteQuestion(Integer questionAggregateId) {
        String functionalityName = new Throwable().getStackTrace()[0].getMethodName();

        switch (workflowType) {
            case SAGAS:
                SagaUnitOfWork sagaUnitOfWork = sagaUnitOfWorkService.createUnitOfWork(functionalityName);
                DeleteQuestionFunctionalitySagas deleteQuestionFunctionalitySagas = new DeleteQuestionFunctionalitySagas(
                        sagaUnitOfWork, sagaUnitOfWorkService, questionService, questionAggregateId);
                deleteQuestionFunctionalitySagas.executeWorkflow(sagaUnitOfWork);
                break;
            default: throw new AnswersException(UNDEFINED_TRANSACTIONAL_MODEL);
        }
    }

    public List<QuestionDto> searchQuestions(String title, String content, Integer courseAggregateId) {
        String functionalityName = new Throwable().getStackTrace()[0].getMethodName();

        switch (workflowType) {
            case SAGAS:
                SagaUnitOfWork sagaUnitOfWork = sagaUnitOfWorkService.createUnitOfWork(functionalityName);
                SearchQuestionsFunctionalitySagas searchQuestionsFunctionalitySagas = new SearchQuestionsFunctionalitySagas(
                        sagaUnitOfWork, sagaUnitOfWorkService, questionService, title, content, courseAggregateId);
                searchQuestionsFunctionalitySagas.executeWorkflow(sagaUnitOfWork);
                return searchQuestionsFunctionalitySagas.getSearchedQuestionDtos();
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