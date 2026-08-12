package pt.ulisboa.tecnico.socialsoftware.answers.microservices.question.coordination.functionalities;

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
import pt.ulisboa.tecnico.socialsoftware.ms.coordination.workflow.command.CommandGateway;
import pt.ulisboa.tecnico.socialsoftware.answers.microservices.question.coordination.sagas.*;
import pt.ulisboa.tecnico.socialsoftware.answers.microservices.question.service.QuestionService;
import pt.ulisboa.tecnico.socialsoftware.answers.shared.dtos.QuestionDto;
import pt.ulisboa.tecnico.socialsoftware.answers.shared.dtos.QuestionTopicDto;
import pt.ulisboa.tecnico.socialsoftware.answers.shared.dtos.OptionDto;
import pt.ulisboa.tecnico.socialsoftware.answers.microservices.question.coordination.webapi.requestDtos.CreateQuestionRequestDto;
import java.util.List;

@Service
public class QuestionFunctionalities {
    @Autowired
    private QuestionService questionService;

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

    public QuestionDto createQuestion(CreateQuestionRequestDto createRequest) {
        String functionalityName = new Throwable().getStackTrace()[0].getMethodName();

        switch (workflowType) {
            case SAGAS:
                SagaUnitOfWork sagaUnitOfWork = sagaUnitOfWorkService.createUnitOfWork(functionalityName);
                checkInput(createRequest);
                CreateQuestionFunctionalitySagas createQuestionFunctionalitySagas = new CreateQuestionFunctionalitySagas(
                        sagaUnitOfWorkService, createRequest, sagaUnitOfWork, commandGateway);
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
                        sagaUnitOfWorkService, questionAggregateId, sagaUnitOfWork, commandGateway);
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
                        sagaUnitOfWorkService, questionDto, sagaUnitOfWork, commandGateway);
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
                        sagaUnitOfWorkService, questionAggregateId, sagaUnitOfWork, commandGateway);
                deleteQuestionFunctionalitySagas.executeWorkflow(sagaUnitOfWork);
                break;
            default: throw new AnswersException(UNDEFINED_TRANSACTIONAL_MODEL);
        }
    }

    public List<QuestionDto> getAllQuestions() {
        String functionalityName = new Throwable().getStackTrace()[0].getMethodName();

        switch (workflowType) {
            case SAGAS:
                SagaUnitOfWork sagaUnitOfWork = sagaUnitOfWorkService.createUnitOfWork(functionalityName);
                GetAllQuestionsFunctionalitySagas getAllQuestionsFunctionalitySagas = new GetAllQuestionsFunctionalitySagas(
                        sagaUnitOfWorkService, sagaUnitOfWork, commandGateway);
                getAllQuestionsFunctionalitySagas.executeWorkflow(sagaUnitOfWork);
                return getAllQuestionsFunctionalitySagas.getQuestions();
            default: throw new AnswersException(UNDEFINED_TRANSACTIONAL_MODEL);
        }
    }

    public QuestionTopicDto addQuestionTopic(Integer questionId, Integer topicAggregateId, QuestionTopicDto topicDto) {
        String functionalityName = new Throwable().getStackTrace()[0].getMethodName();

        switch (workflowType) {
            case SAGAS:
                SagaUnitOfWork sagaUnitOfWork = sagaUnitOfWorkService.createUnitOfWork(functionalityName);
                AddQuestionTopicFunctionalitySagas addQuestionTopicFunctionalitySagas = new AddQuestionTopicFunctionalitySagas(
                        sagaUnitOfWorkService,
                        questionId, topicAggregateId, topicDto,
                        sagaUnitOfWork, commandGateway);
                addQuestionTopicFunctionalitySagas.executeWorkflow(sagaUnitOfWork);
                return addQuestionTopicFunctionalitySagas.getAddedTopicDto();
            default: throw new AnswersException(UNDEFINED_TRANSACTIONAL_MODEL);
        }
    }

    public List<QuestionTopicDto> addQuestionTopics(Integer questionId, List<QuestionTopicDto> topicDtos) {
        String functionalityName = new Throwable().getStackTrace()[0].getMethodName();

        switch (workflowType) {
            case SAGAS:
                SagaUnitOfWork sagaUnitOfWork = sagaUnitOfWorkService.createUnitOfWork(functionalityName);
                AddQuestionTopicsFunctionalitySagas addQuestionTopicsFunctionalitySagas = new AddQuestionTopicsFunctionalitySagas(
                        sagaUnitOfWorkService,
                        questionId, topicDtos,
                        sagaUnitOfWork, commandGateway);
                addQuestionTopicsFunctionalitySagas.executeWorkflow(sagaUnitOfWork);
                return addQuestionTopicsFunctionalitySagas.getAddedTopicDtos();
            default: throw new AnswersException(UNDEFINED_TRANSACTIONAL_MODEL);
        }
    }

    public QuestionTopicDto getQuestionTopic(Integer questionId, Integer topicAggregateId) {
        String functionalityName = new Throwable().getStackTrace()[0].getMethodName();

        switch (workflowType) {
            case SAGAS:
                SagaUnitOfWork sagaUnitOfWork = sagaUnitOfWorkService.createUnitOfWork(functionalityName);
                GetQuestionTopicFunctionalitySagas getQuestionTopicFunctionalitySagas = new GetQuestionTopicFunctionalitySagas(
                        sagaUnitOfWorkService,
                        questionId, topicAggregateId,
                        sagaUnitOfWork, commandGateway);
                getQuestionTopicFunctionalitySagas.executeWorkflow(sagaUnitOfWork);
                return getQuestionTopicFunctionalitySagas.getTopicDto();
            default: throw new AnswersException(UNDEFINED_TRANSACTIONAL_MODEL);
        }
    }

    public QuestionTopicDto updateQuestionTopic(Integer questionId, Integer topicAggregateId, QuestionTopicDto topicDto) {
        String functionalityName = new Throwable().getStackTrace()[0].getMethodName();

        switch (workflowType) {
            case SAGAS:
                SagaUnitOfWork sagaUnitOfWork = sagaUnitOfWorkService.createUnitOfWork(functionalityName);
                UpdateQuestionTopicFunctionalitySagas updateQuestionTopicFunctionalitySagas = new UpdateQuestionTopicFunctionalitySagas(
                        sagaUnitOfWorkService,
                        questionId, topicAggregateId, topicDto,
                        sagaUnitOfWork, commandGateway);
                updateQuestionTopicFunctionalitySagas.executeWorkflow(sagaUnitOfWork);
                return updateQuestionTopicFunctionalitySagas.getUpdatedTopicDto();
            default: throw new AnswersException(UNDEFINED_TRANSACTIONAL_MODEL);
        }
    }

    public void removeQuestionTopic(Integer questionId, Integer topicAggregateId) {
        String functionalityName = new Throwable().getStackTrace()[0].getMethodName();

        switch (workflowType) {
            case SAGAS:
                SagaUnitOfWork sagaUnitOfWork = sagaUnitOfWorkService.createUnitOfWork(functionalityName);
                RemoveQuestionTopicFunctionalitySagas removeQuestionTopicFunctionalitySagas = new RemoveQuestionTopicFunctionalitySagas(
                        sagaUnitOfWorkService,
                        questionId, topicAggregateId,
                        sagaUnitOfWork, commandGateway);
                removeQuestionTopicFunctionalitySagas.executeWorkflow(sagaUnitOfWork);
                break;
            default: throw new AnswersException(UNDEFINED_TRANSACTIONAL_MODEL);
        }
    }

    public OptionDto addQuestionOption(Integer questionId, Integer key, OptionDto optionDto) {
        String functionalityName = new Throwable().getStackTrace()[0].getMethodName();

        switch (workflowType) {
            case SAGAS:
                SagaUnitOfWork sagaUnitOfWork = sagaUnitOfWorkService.createUnitOfWork(functionalityName);
                AddQuestionOptionFunctionalitySagas addQuestionOptionFunctionalitySagas = new AddQuestionOptionFunctionalitySagas(
                        sagaUnitOfWorkService,
                        questionId, key, optionDto,
                        sagaUnitOfWork, commandGateway);
                addQuestionOptionFunctionalitySagas.executeWorkflow(sagaUnitOfWork);
                return addQuestionOptionFunctionalitySagas.getAddedOptionDto();
            default: throw new AnswersException(UNDEFINED_TRANSACTIONAL_MODEL);
        }
    }

    public List<OptionDto> addQuestionOptions(Integer questionId, List<OptionDto> optionDtos) {
        String functionalityName = new Throwable().getStackTrace()[0].getMethodName();

        switch (workflowType) {
            case SAGAS:
                SagaUnitOfWork sagaUnitOfWork = sagaUnitOfWorkService.createUnitOfWork(functionalityName);
                AddQuestionOptionsFunctionalitySagas addQuestionOptionsFunctionalitySagas = new AddQuestionOptionsFunctionalitySagas(
                        sagaUnitOfWorkService,
                        questionId, optionDtos,
                        sagaUnitOfWork, commandGateway);
                addQuestionOptionsFunctionalitySagas.executeWorkflow(sagaUnitOfWork);
                return addQuestionOptionsFunctionalitySagas.getAddedOptionDtos();
            default: throw new AnswersException(UNDEFINED_TRANSACTIONAL_MODEL);
        }
    }

    public OptionDto getQuestionOption(Integer questionId, Integer key) {
        String functionalityName = new Throwable().getStackTrace()[0].getMethodName();

        switch (workflowType) {
            case SAGAS:
                SagaUnitOfWork sagaUnitOfWork = sagaUnitOfWorkService.createUnitOfWork(functionalityName);
                GetQuestionOptionFunctionalitySagas getQuestionOptionFunctionalitySagas = new GetQuestionOptionFunctionalitySagas(
                        sagaUnitOfWorkService,
                        questionId, key,
                        sagaUnitOfWork, commandGateway);
                getQuestionOptionFunctionalitySagas.executeWorkflow(sagaUnitOfWork);
                return getQuestionOptionFunctionalitySagas.getOptionDto();
            default: throw new AnswersException(UNDEFINED_TRANSACTIONAL_MODEL);
        }
    }

    public OptionDto updateQuestionOption(Integer questionId, Integer key, OptionDto optionDto) {
        String functionalityName = new Throwable().getStackTrace()[0].getMethodName();

        switch (workflowType) {
            case SAGAS:
                SagaUnitOfWork sagaUnitOfWork = sagaUnitOfWorkService.createUnitOfWork(functionalityName);
                UpdateQuestionOptionFunctionalitySagas updateQuestionOptionFunctionalitySagas = new UpdateQuestionOptionFunctionalitySagas(
                        sagaUnitOfWorkService,
                        questionId, key, optionDto,
                        sagaUnitOfWork, commandGateway);
                updateQuestionOptionFunctionalitySagas.executeWorkflow(sagaUnitOfWork);
                return updateQuestionOptionFunctionalitySagas.getUpdatedOptionDto();
            default: throw new AnswersException(UNDEFINED_TRANSACTIONAL_MODEL);
        }
    }

    public void removeQuestionOption(Integer questionId, Integer key) {
        String functionalityName = new Throwable().getStackTrace()[0].getMethodName();

        switch (workflowType) {
            case SAGAS:
                SagaUnitOfWork sagaUnitOfWork = sagaUnitOfWorkService.createUnitOfWork(functionalityName);
                RemoveQuestionOptionFunctionalitySagas removeQuestionOptionFunctionalitySagas = new RemoveQuestionOptionFunctionalitySagas(
                        sagaUnitOfWorkService,
                        questionId, key,
                        sagaUnitOfWork, commandGateway);
                removeQuestionOptionFunctionalitySagas.executeWorkflow(sagaUnitOfWork);
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

    private void checkInput(CreateQuestionRequestDto createRequest) {
        if (createRequest.getTitle() == null) {
            throw new AnswersException(QUESTION_MISSING_TITLE);
        }
        if (createRequest.getContent() == null) {
            throw new AnswersException(QUESTION_MISSING_CONTENT);
        }
}
}