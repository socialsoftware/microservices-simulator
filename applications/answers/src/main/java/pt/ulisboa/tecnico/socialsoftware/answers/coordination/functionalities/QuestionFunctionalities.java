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
import pt.ulisboa.tecnico.socialsoftware.answers.shared.dtos.QuestionTopicDto;
import pt.ulisboa.tecnico.socialsoftware.answers.shared.dtos.OptionDto;
import pt.ulisboa.tecnico.socialsoftware.answers.coordination.webapi.requestDtos.CreateQuestionRequestDto;
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

    public QuestionDto createQuestion(CreateQuestionRequestDto createRequest) {
        String functionalityName = new Throwable().getStackTrace()[0].getMethodName();

        switch (workflowType) {
            case SAGAS:
                SagaUnitOfWork sagaUnitOfWork = sagaUnitOfWorkService.createUnitOfWork(functionalityName);
                checkInput(createRequest);
                CreateQuestionFunctionalitySagas createQuestionFunctionalitySagas = new CreateQuestionFunctionalitySagas(
                        sagaUnitOfWork, sagaUnitOfWorkService, questionService, createRequest);
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

    public List<QuestionDto> getAllQuestions() {
        String functionalityName = new Throwable().getStackTrace()[0].getMethodName();

        switch (workflowType) {
            case SAGAS:
                SagaUnitOfWork sagaUnitOfWork = sagaUnitOfWorkService.createUnitOfWork(functionalityName);
                GetAllQuestionsFunctionalitySagas getAllQuestionsFunctionalitySagas = new GetAllQuestionsFunctionalitySagas(
                        sagaUnitOfWork, sagaUnitOfWorkService, questionService);
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
                        sagaUnitOfWork, sagaUnitOfWorkService, questionService,
                        questionId, topicAggregateId, topicDto);
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
                        sagaUnitOfWork, sagaUnitOfWorkService, questionService,
                        questionId, topicDtos);
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
                        sagaUnitOfWork, sagaUnitOfWorkService, questionService,
                        questionId, topicAggregateId);
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
                        sagaUnitOfWork, sagaUnitOfWorkService, questionService,
                        questionId, topicAggregateId, topicDto);
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
                        sagaUnitOfWork, sagaUnitOfWorkService, questionService,
                        questionId, topicAggregateId);
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
                        sagaUnitOfWork, sagaUnitOfWorkService, questionService,
                        questionId, key, optionDto);
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
                        sagaUnitOfWork, sagaUnitOfWorkService, questionService,
                        questionId, optionDtos);
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
                        sagaUnitOfWork, sagaUnitOfWorkService, questionService,
                        questionId, key);
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
                        sagaUnitOfWork, sagaUnitOfWorkService, questionService,
                        questionId, key, optionDto);
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
                        sagaUnitOfWork, sagaUnitOfWorkService, questionService,
                        questionId, key);
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