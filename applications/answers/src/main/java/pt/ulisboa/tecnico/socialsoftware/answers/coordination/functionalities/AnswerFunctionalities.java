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
import pt.ulisboa.tecnico.socialsoftware.answers.sagas.coordination.answer.*;
import pt.ulisboa.tecnico.socialsoftware.answers.microservices.answer.service.AnswerService;
import pt.ulisboa.tecnico.socialsoftware.answers.shared.dtos.AnswerDto;
import pt.ulisboa.tecnico.socialsoftware.answers.shared.dtos.AnswerQuestionDto;
import pt.ulisboa.tecnico.socialsoftware.answers.coordination.webapi.requestDtos.CreateAnswerRequestDto;
import java.util.List;

@Service
public class AnswerFunctionalities {
    @Autowired
    private AnswerService answerService;

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

    public AnswerDto createAnswer(CreateAnswerRequestDto createRequest) {
        String functionalityName = new Throwable().getStackTrace()[0].getMethodName();

        switch (workflowType) {
            case SAGAS:
                SagaUnitOfWork sagaUnitOfWork = sagaUnitOfWorkService.createUnitOfWork(functionalityName);
                checkInput(createRequest);
                CreateAnswerFunctionalitySagas createAnswerFunctionalitySagas = new CreateAnswerFunctionalitySagas(
                        sagaUnitOfWork, sagaUnitOfWorkService, answerService, createRequest);
                createAnswerFunctionalitySagas.executeWorkflow(sagaUnitOfWork);
                return createAnswerFunctionalitySagas.getCreatedAnswerDto();
            default: throw new AnswersException(UNDEFINED_TRANSACTIONAL_MODEL);
        }
    }

    public AnswerDto getAnswerById(Integer answerAggregateId) {
        String functionalityName = new Throwable().getStackTrace()[0].getMethodName();

        switch (workflowType) {
            case SAGAS:
                SagaUnitOfWork sagaUnitOfWork = sagaUnitOfWorkService.createUnitOfWork(functionalityName);
                GetAnswerByIdFunctionalitySagas getAnswerByIdFunctionalitySagas = new GetAnswerByIdFunctionalitySagas(
                        sagaUnitOfWork, sagaUnitOfWorkService, answerService, answerAggregateId);
                getAnswerByIdFunctionalitySagas.executeWorkflow(sagaUnitOfWork);
                return getAnswerByIdFunctionalitySagas.getAnswerDto();
            default: throw new AnswersException(UNDEFINED_TRANSACTIONAL_MODEL);
        }
    }

    public AnswerDto updateAnswer(AnswerDto answerDto) {
        String functionalityName = new Throwable().getStackTrace()[0].getMethodName();

        switch (workflowType) {
            case SAGAS:
                SagaUnitOfWork sagaUnitOfWork = sagaUnitOfWorkService.createUnitOfWork(functionalityName);
                checkInput(answerDto);
                UpdateAnswerFunctionalitySagas updateAnswerFunctionalitySagas = new UpdateAnswerFunctionalitySagas(
                        sagaUnitOfWork, sagaUnitOfWorkService, answerService, answerDto);
                updateAnswerFunctionalitySagas.executeWorkflow(sagaUnitOfWork);
                return updateAnswerFunctionalitySagas.getUpdatedAnswerDto();
            default: throw new AnswersException(UNDEFINED_TRANSACTIONAL_MODEL);
        }
    }

    public void deleteAnswer(Integer answerAggregateId) {
        String functionalityName = new Throwable().getStackTrace()[0].getMethodName();

        switch (workflowType) {
            case SAGAS:
                SagaUnitOfWork sagaUnitOfWork = sagaUnitOfWorkService.createUnitOfWork(functionalityName);
                DeleteAnswerFunctionalitySagas deleteAnswerFunctionalitySagas = new DeleteAnswerFunctionalitySagas(
                        sagaUnitOfWork, sagaUnitOfWorkService, answerService, answerAggregateId);
                deleteAnswerFunctionalitySagas.executeWorkflow(sagaUnitOfWork);
                break;
            default: throw new AnswersException(UNDEFINED_TRANSACTIONAL_MODEL);
        }
    }

    public List<AnswerDto> getAllAnswers() {
        String functionalityName = new Throwable().getStackTrace()[0].getMethodName();

        switch (workflowType) {
            case SAGAS:
                SagaUnitOfWork sagaUnitOfWork = sagaUnitOfWorkService.createUnitOfWork(functionalityName);
                GetAllAnswersFunctionalitySagas getAllAnswersFunctionalitySagas = new GetAllAnswersFunctionalitySagas(
                        sagaUnitOfWork, sagaUnitOfWorkService, answerService);
                getAllAnswersFunctionalitySagas.executeWorkflow(sagaUnitOfWork);
                return getAllAnswersFunctionalitySagas.getAnswers();
            default: throw new AnswersException(UNDEFINED_TRANSACTIONAL_MODEL);
        }
    }

    public AnswerQuestionDto addAnswerQuestion(Integer answerId, Integer questionAggregateId, AnswerQuestionDto questionDto) {
        String functionalityName = new Throwable().getStackTrace()[0].getMethodName();

        switch (workflowType) {
            case SAGAS:
                SagaUnitOfWork sagaUnitOfWork = sagaUnitOfWorkService.createUnitOfWork(functionalityName);
                AddAnswerQuestionFunctionalitySagas addAnswerQuestionFunctionalitySagas = new AddAnswerQuestionFunctionalitySagas(
                        sagaUnitOfWork, sagaUnitOfWorkService, answerService,
                        answerId, questionAggregateId, questionDto);
                addAnswerQuestionFunctionalitySagas.executeWorkflow(sagaUnitOfWork);
                return addAnswerQuestionFunctionalitySagas.getAddedQuestionDto();
            default: throw new AnswersException(UNDEFINED_TRANSACTIONAL_MODEL);
        }
    }

    public List<AnswerQuestionDto> addAnswerQuestions(Integer answerId, List<AnswerQuestionDto> questionDtos) {
        String functionalityName = new Throwable().getStackTrace()[0].getMethodName();

        switch (workflowType) {
            case SAGAS:
                SagaUnitOfWork sagaUnitOfWork = sagaUnitOfWorkService.createUnitOfWork(functionalityName);
                AddAnswerQuestionsFunctionalitySagas addAnswerQuestionsFunctionalitySagas = new AddAnswerQuestionsFunctionalitySagas(
                        sagaUnitOfWork, sagaUnitOfWorkService, answerService,
                        answerId, questionDtos);
                addAnswerQuestionsFunctionalitySagas.executeWorkflow(sagaUnitOfWork);
                return addAnswerQuestionsFunctionalitySagas.getAddedQuestionDtos();
            default: throw new AnswersException(UNDEFINED_TRANSACTIONAL_MODEL);
        }
    }

    public AnswerQuestionDto getAnswerQuestion(Integer answerId, Integer questionAggregateId) {
        String functionalityName = new Throwable().getStackTrace()[0].getMethodName();

        switch (workflowType) {
            case SAGAS:
                SagaUnitOfWork sagaUnitOfWork = sagaUnitOfWorkService.createUnitOfWork(functionalityName);
                GetAnswerQuestionFunctionalitySagas getAnswerQuestionFunctionalitySagas = new GetAnswerQuestionFunctionalitySagas(
                        sagaUnitOfWork, sagaUnitOfWorkService, answerService,
                        answerId, questionAggregateId);
                getAnswerQuestionFunctionalitySagas.executeWorkflow(sagaUnitOfWork);
                return getAnswerQuestionFunctionalitySagas.getQuestionDto();
            default: throw new AnswersException(UNDEFINED_TRANSACTIONAL_MODEL);
        }
    }

    public AnswerQuestionDto updateAnswerQuestion(Integer answerId, Integer questionAggregateId, AnswerQuestionDto questionDto) {
        String functionalityName = new Throwable().getStackTrace()[0].getMethodName();

        switch (workflowType) {
            case SAGAS:
                SagaUnitOfWork sagaUnitOfWork = sagaUnitOfWorkService.createUnitOfWork(functionalityName);
                UpdateAnswerQuestionFunctionalitySagas updateAnswerQuestionFunctionalitySagas = new UpdateAnswerQuestionFunctionalitySagas(
                        sagaUnitOfWork, sagaUnitOfWorkService, answerService,
                        answerId, questionAggregateId, questionDto);
                updateAnswerQuestionFunctionalitySagas.executeWorkflow(sagaUnitOfWork);
                return updateAnswerQuestionFunctionalitySagas.getUpdatedQuestionDto();
            default: throw new AnswersException(UNDEFINED_TRANSACTIONAL_MODEL);
        }
    }

    public void removeAnswerQuestion(Integer answerId, Integer questionAggregateId) {
        String functionalityName = new Throwable().getStackTrace()[0].getMethodName();

        switch (workflowType) {
            case SAGAS:
                SagaUnitOfWork sagaUnitOfWork = sagaUnitOfWorkService.createUnitOfWork(functionalityName);
                RemoveAnswerQuestionFunctionalitySagas removeAnswerQuestionFunctionalitySagas = new RemoveAnswerQuestionFunctionalitySagas(
                        sagaUnitOfWork, sagaUnitOfWorkService, answerService,
                        answerId, questionAggregateId);
                removeAnswerQuestionFunctionalitySagas.executeWorkflow(sagaUnitOfWork);
                break;
            default: throw new AnswersException(UNDEFINED_TRANSACTIONAL_MODEL);
        }
    }

    private void checkInput(AnswerDto answerDto) {
}

    private void checkInput(CreateAnswerRequestDto createRequest) {
}
}