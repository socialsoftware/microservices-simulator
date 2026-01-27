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
import pt.ulisboa.tecnico.socialsoftware.answers.shared.dtos.QuizDto;
import pt.ulisboa.tecnico.socialsoftware.answers.coordination.webapi.requestDtos.CreateQuizRequestDto;
import java.util.List;

@Service
public class QuizFunctionalities {
    @Autowired
    private QuizService quizService;

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

    public QuizDto createQuiz(CreateQuizRequestDto createRequest) {
        String functionalityName = new Throwable().getStackTrace()[0].getMethodName();

        switch (workflowType) {
            case SAGAS:
                SagaUnitOfWork sagaUnitOfWork = sagaUnitOfWorkService.createUnitOfWork(functionalityName);
                checkInput(createRequest);
                CreateQuizFunctionalitySagas createQuizFunctionalitySagas = new CreateQuizFunctionalitySagas(
                        sagaUnitOfWork, sagaUnitOfWorkService, quizService, createRequest);
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

    public List<QuizDto> getAllQuizs() {
        String functionalityName = new Throwable().getStackTrace()[0].getMethodName();

        switch (workflowType) {
            case SAGAS:
                SagaUnitOfWork sagaUnitOfWork = sagaUnitOfWorkService.createUnitOfWork(functionalityName);
                GetAllQuizsFunctionalitySagas getAllQuizsFunctionalitySagas = new GetAllQuizsFunctionalitySagas(
                        sagaUnitOfWork, sagaUnitOfWorkService, quizService);
                getAllQuizsFunctionalitySagas.executeWorkflow(sagaUnitOfWork);
                return getAllQuizsFunctionalitySagas.getQuizs();
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