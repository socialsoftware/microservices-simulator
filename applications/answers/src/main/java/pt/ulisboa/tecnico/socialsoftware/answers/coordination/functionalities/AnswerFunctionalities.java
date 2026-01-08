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
import pt.ulisboa.tecnico.socialsoftware.answers.microservices.execution.service.ExecutionService;
import pt.ulisboa.tecnico.socialsoftware.answers.microservices.user.service.UserService;
import pt.ulisboa.tecnico.socialsoftware.answers.microservices.quiz.service.QuizService;
import pt.ulisboa.tecnico.socialsoftware.answers.shared.dtos.AnswerDto;
import java.util.List;

@Service
public class AnswerFunctionalities {
    @Autowired
    private AnswerService answerService;

    @Autowired
    private ExecutionService executionService;

    @Autowired
    private UserService userService;

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

    public AnswerDto createAnswer(AnswerDto answerDto) {
        String functionalityName = new Throwable().getStackTrace()[0].getMethodName();

        switch (workflowType) {
            case SAGAS:
                SagaUnitOfWork sagaUnitOfWork = sagaUnitOfWorkService.createUnitOfWork(functionalityName);
                checkInput(answerDto);
                CreateAnswerFunctionalitySagas createAnswerFunctionalitySagas = new CreateAnswerFunctionalitySagas(
                        sagaUnitOfWork, sagaUnitOfWorkService, answerService, answerDto);
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

    public List<AnswerDto> searchAnswers(Boolean completed, Integer executionAggregateId, Integer userAggregateId, Integer quizAggregateId) {
        String functionalityName = new Throwable().getStackTrace()[0].getMethodName();

        switch (workflowType) {
            case SAGAS:
                SagaUnitOfWork sagaUnitOfWork = sagaUnitOfWorkService.createUnitOfWork(functionalityName);
                SearchAnswersFunctionalitySagas searchAnswersFunctionalitySagas = new SearchAnswersFunctionalitySagas(
                        sagaUnitOfWork, sagaUnitOfWorkService, answerService, completed, executionAggregateId, userAggregateId, quizAggregateId);
                searchAnswersFunctionalitySagas.executeWorkflow(sagaUnitOfWork);
                return searchAnswersFunctionalitySagas.getSearchedAnswerDtos();
            default: throw new AnswersException(UNDEFINED_TRANSACTIONAL_MODEL);
        }
    }

}