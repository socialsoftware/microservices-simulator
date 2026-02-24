package pt.ulisboa.tecnico.socialsoftware.answers.microservices.quiz.coordination.functionalities;

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
import pt.ulisboa.tecnico.socialsoftware.ms.coordination.workflow.CommandGateway;
import pt.ulisboa.tecnico.socialsoftware.answers.microservices.quiz.coordination.sagas.*;
import pt.ulisboa.tecnico.socialsoftware.answers.microservices.quiz.service.QuizService;
import pt.ulisboa.tecnico.socialsoftware.answers.shared.dtos.QuizDto;
import pt.ulisboa.tecnico.socialsoftware.answers.shared.dtos.QuizQuestionDto;
import pt.ulisboa.tecnico.socialsoftware.answers.microservices.quiz.coordination.webapi.requestDtos.CreateQuizRequestDto;
import java.util.List;

@Service
public class QuizFunctionalities {
    @Autowired
    private QuizService quizService;

    @Autowired
    private SagaUnitOfWorkService sagaUnitOfWorkService;

    @Autowired
    private CommandGateway commandGateway;


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

    public QuizDto createQuiz(CreateQuizRequestDto createRequest) {
        String functionalityName = new Throwable().getStackTrace()[0].getMethodName();

        switch (workflowType) {
            case SAGAS:
                SagaUnitOfWork sagaUnitOfWork = sagaUnitOfWorkService.createUnitOfWork(functionalityName);
                checkInput(createRequest);
                CreateQuizFunctionalitySagas createQuizFunctionalitySagas = new CreateQuizFunctionalitySagas(
                        sagaUnitOfWorkService, createRequest, sagaUnitOfWork, commandGateway);
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
                        sagaUnitOfWorkService, quizAggregateId, sagaUnitOfWork, commandGateway);
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
                        sagaUnitOfWorkService, quizDto, sagaUnitOfWork, commandGateway);
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
                        sagaUnitOfWorkService, quizAggregateId, sagaUnitOfWork, commandGateway);
                deleteQuizFunctionalitySagas.executeWorkflow(sagaUnitOfWork);
                break;
            default: throw new AnswersException(UNDEFINED_TRANSACTIONAL_MODEL);
        }
    }

    public List<QuizDto> getAllQuizs() {
        String functionalityName = new Throwable().getStackTrace()[0].getMethodName();

        switch (workflowType) {
            case SAGAS:
                SagaUnitOfWork sagaUnitOfWork = sagaUnitOfWorkService.createUnitOfWork(functionalityName);
                GetAllQuizsFunctionalitySagas getAllQuizsFunctionalitySagas = new GetAllQuizsFunctionalitySagas(
                        sagaUnitOfWorkService, sagaUnitOfWork, commandGateway);
                getAllQuizsFunctionalitySagas.executeWorkflow(sagaUnitOfWork);
                return getAllQuizsFunctionalitySagas.getQuizs();
            default: throw new AnswersException(UNDEFINED_TRANSACTIONAL_MODEL);
        }
    }

    public QuizQuestionDto addQuizQuestion(Integer quizId, Integer questionAggregateId, QuizQuestionDto questionDto) {
        String functionalityName = new Throwable().getStackTrace()[0].getMethodName();

        switch (workflowType) {
            case SAGAS:
                SagaUnitOfWork sagaUnitOfWork = sagaUnitOfWorkService.createUnitOfWork(functionalityName);
                AddQuizQuestionFunctionalitySagas addQuizQuestionFunctionalitySagas = new AddQuizQuestionFunctionalitySagas(
                        sagaUnitOfWorkService,
                        quizId, questionAggregateId, questionDto,
                        sagaUnitOfWork, commandGateway);
                addQuizQuestionFunctionalitySagas.executeWorkflow(sagaUnitOfWork);
                return addQuizQuestionFunctionalitySagas.getAddedQuestionDto();
            default: throw new AnswersException(UNDEFINED_TRANSACTIONAL_MODEL);
        }
    }

    public List<QuizQuestionDto> addQuizQuestions(Integer quizId, List<QuizQuestionDto> questionDtos) {
        String functionalityName = new Throwable().getStackTrace()[0].getMethodName();

        switch (workflowType) {
            case SAGAS:
                SagaUnitOfWork sagaUnitOfWork = sagaUnitOfWorkService.createUnitOfWork(functionalityName);
                AddQuizQuestionsFunctionalitySagas addQuizQuestionsFunctionalitySagas = new AddQuizQuestionsFunctionalitySagas(
                        sagaUnitOfWorkService,
                        quizId, questionDtos,
                        sagaUnitOfWork, commandGateway);
                addQuizQuestionsFunctionalitySagas.executeWorkflow(sagaUnitOfWork);
                return addQuizQuestionsFunctionalitySagas.getAddedQuestionDtos();
            default: throw new AnswersException(UNDEFINED_TRANSACTIONAL_MODEL);
        }
    }

    public QuizQuestionDto getQuizQuestion(Integer quizId, Integer questionAggregateId) {
        String functionalityName = new Throwable().getStackTrace()[0].getMethodName();

        switch (workflowType) {
            case SAGAS:
                SagaUnitOfWork sagaUnitOfWork = sagaUnitOfWorkService.createUnitOfWork(functionalityName);
                GetQuizQuestionFunctionalitySagas getQuizQuestionFunctionalitySagas = new GetQuizQuestionFunctionalitySagas(
                        sagaUnitOfWorkService,
                        quizId, questionAggregateId,
                        sagaUnitOfWork, commandGateway);
                getQuizQuestionFunctionalitySagas.executeWorkflow(sagaUnitOfWork);
                return getQuizQuestionFunctionalitySagas.getQuestionDto();
            default: throw new AnswersException(UNDEFINED_TRANSACTIONAL_MODEL);
        }
    }

    public QuizQuestionDto updateQuizQuestion(Integer quizId, Integer questionAggregateId, QuizQuestionDto questionDto) {
        String functionalityName = new Throwable().getStackTrace()[0].getMethodName();

        switch (workflowType) {
            case SAGAS:
                SagaUnitOfWork sagaUnitOfWork = sagaUnitOfWorkService.createUnitOfWork(functionalityName);
                UpdateQuizQuestionFunctionalitySagas updateQuizQuestionFunctionalitySagas = new UpdateQuizQuestionFunctionalitySagas(
                        sagaUnitOfWorkService,
                        quizId, questionAggregateId, questionDto,
                        sagaUnitOfWork, commandGateway);
                updateQuizQuestionFunctionalitySagas.executeWorkflow(sagaUnitOfWork);
                return updateQuizQuestionFunctionalitySagas.getUpdatedQuestionDto();
            default: throw new AnswersException(UNDEFINED_TRANSACTIONAL_MODEL);
        }
    }

    public void removeQuizQuestion(Integer quizId, Integer questionAggregateId) {
        String functionalityName = new Throwable().getStackTrace()[0].getMethodName();

        switch (workflowType) {
            case SAGAS:
                SagaUnitOfWork sagaUnitOfWork = sagaUnitOfWorkService.createUnitOfWork(functionalityName);
                RemoveQuizQuestionFunctionalitySagas removeQuizQuestionFunctionalitySagas = new RemoveQuizQuestionFunctionalitySagas(
                        sagaUnitOfWorkService,
                        quizId, questionAggregateId,
                        sagaUnitOfWork, commandGateway);
                removeQuizQuestionFunctionalitySagas.executeWorkflow(sagaUnitOfWork);
                break;
            default: throw new AnswersException(UNDEFINED_TRANSACTIONAL_MODEL);
        }
    }

    private void checkInput(QuizDto quizDto) {
        if (quizDto.getTitle() == null) {
            throw new AnswersException(QUIZ_MISSING_TITLE);
        }
}

    private void checkInput(CreateQuizRequestDto createRequest) {
        if (createRequest.getTitle() == null) {
            throw new AnswersException(QUIZ_MISSING_TITLE);
        }
}
}