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
import pt.ulisboa.tecnico.socialsoftware.answers.sagas.coordination.quiz.*;
import pt.ulisboa.tecnico.socialsoftware.answers.microservices.quiz.service.QuizService;
import pt.ulisboa.tecnico.socialsoftware.answers.microservices.execution.service.ExecutionService;
import pt.ulisboa.tecnico.socialsoftware.answers.microservices.question.service.QuestionService;
import pt.ulisboa.tecnico.socialsoftware.answers.shared.dtos.QuizDto;
import java.util.List;
import pt.ulisboa.tecnico.socialsoftware.answers.shared.enums.QuizType;

@Service
public class QuizFunctionalities {
    @Autowired
    private QuizService quizService;

    @Autowired
    private ExecutionService executionService;

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

    public QuizDto createQuiz(QuizDto quizDto) {
        String functionalityName = new Throwable().getStackTrace()[0].getMethodName();

        switch (workflowType) {
            case SAGAS:
                SagaUnitOfWork sagaUnitOfWork = sagaUnitOfWorkService.createUnitOfWork(functionalityName);
                checkInput(quizDto);
                CreateQuizFunctionalitySagas createQuizFunctionalitySagas = new CreateQuizFunctionalitySagas(
                        sagaUnitOfWork, sagaUnitOfWorkService, quizService, executionService, questionService, quizDto);
                createQuizFunctionalitySagas.executeWorkflow(sagaUnitOfWork);
                return createQuizFunctionalitySagas.getCreatedQuizDto();
            default: throw new AnswersException(UNDEFINED_TRANSACTIONAL_MODEL);
        }
    }

    public QuizDto getQuizById(Integer quizAggregateId) {
        String functionalityName = new Throwable().getStackTrace()[0].getMethodName();

        switch (workflowType) {
            case SAGAS:
                SagaUnitOfWork sagaUnitOfWork = sagaUnitOfWorkService.createUnitOfWork(functionalityName);
                GetQuizByIdFunctionalitySagas getQuizByIdFunctionalitySagas = new GetQuizByIdFunctionalitySagas(
                        sagaUnitOfWork, sagaUnitOfWorkService, quizService, quizAggregateId);
                getQuizByIdFunctionalitySagas.executeWorkflow(sagaUnitOfWork);
                return getQuizByIdFunctionalitySagas.getQuizDto();
            default: throw new AnswersException(UNDEFINED_TRANSACTIONAL_MODEL);
        }
    }

    public QuizDto updateQuiz(QuizDto quizDto) {
        String functionalityName = new Throwable().getStackTrace()[0].getMethodName();

        switch (workflowType) {
            case SAGAS:
                SagaUnitOfWork sagaUnitOfWork = sagaUnitOfWorkService.createUnitOfWork(functionalityName);
                checkInput(quizDto);
                UpdateQuizFunctionalitySagas updateQuizFunctionalitySagas = new UpdateQuizFunctionalitySagas(
                        sagaUnitOfWork, sagaUnitOfWorkService, quizService, quizDto);
                updateQuizFunctionalitySagas.executeWorkflow(sagaUnitOfWork);
                return updateQuizFunctionalitySagas.getUpdatedQuizDto();
            default: throw new AnswersException(UNDEFINED_TRANSACTIONAL_MODEL);
        }
    }

    public void deleteQuiz(Integer quizAggregateId) {
        String functionalityName = new Throwable().getStackTrace()[0].getMethodName();

        switch (workflowType) {
            case SAGAS:
                SagaUnitOfWork sagaUnitOfWork = sagaUnitOfWorkService.createUnitOfWork(functionalityName);
                DeleteQuizFunctionalitySagas deleteQuizFunctionalitySagas = new DeleteQuizFunctionalitySagas(
                        sagaUnitOfWork, sagaUnitOfWorkService, quizService, quizAggregateId);
                deleteQuizFunctionalitySagas.executeWorkflow(sagaUnitOfWork);
                break;
            default: throw new AnswersException(UNDEFINED_TRANSACTIONAL_MODEL);
        }
    }

    public List<QuizDto> searchQuizs(String title, QuizType quizType, Integer executionAggregateId) {
        String functionalityName = new Throwable().getStackTrace()[0].getMethodName();

        switch (workflowType) {
            case SAGAS:
                SagaUnitOfWork sagaUnitOfWork = sagaUnitOfWorkService.createUnitOfWork(functionalityName);
                SearchQuizsFunctionalitySagas searchQuizsFunctionalitySagas = new SearchQuizsFunctionalitySagas(
                        sagaUnitOfWork, sagaUnitOfWorkService, quizService, title, quizType, executionAggregateId);
                searchQuizsFunctionalitySagas.executeWorkflow(sagaUnitOfWork);
                return searchQuizsFunctionalitySagas.getSearchedQuizDtos();
            default: throw new AnswersException(UNDEFINED_TRANSACTIONAL_MODEL);
        }
    }

    private void checkInput(QuizDto quizDto) {
        if (quizDto.getTitle() == null) {
            throw new AnswersException(QUIZ_MISSING_TITLE);
        }
    }
}