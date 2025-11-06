package pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.question.commandHandler;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import pt.ulisboa.tecnico.socialsoftware.ms.coordination.workflow.Command;
import pt.ulisboa.tecnico.socialsoftware.ms.coordination.workflow.CommandHandler;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.unitOfWork.SagaUnitOfWork;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.unitOfWork.SagaUnitOfWorkService;
import pt.ulisboa.tecnico.socialsoftware.quizzes.command.question.*;
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.question.aggregate.QuestionDto;
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.question.service.QuestionService;

import java.util.logging.Logger;

@Component
public class QuestionCommandHandler implements CommandHandler {
    private static final Logger logger = Logger.getLogger(QuestionCommandHandler.class.getName());

    @Autowired
    private QuestionService questionService;

    @Autowired(required = false)
    private SagaUnitOfWorkService sagaUnitOfWorkService;

    @Override
    public Object handle(Command command) {
        if (command.getForbiddenStates() != null && !command.getForbiddenStates().isEmpty()) {
            sagaUnitOfWorkService.verifySagaState(command.getRootAggregateId(), command.getForbiddenStates());
        }
        Object returnObject;
        switch (command) {
            case CreateQuestionCommand createQuestionCommand -> returnObject = handleCreateQuestion(createQuestionCommand);
            case UpdateQuestionCommand updateQuestionCommand -> returnObject = handleUpdateQuestion(updateQuestionCommand);
            case RemoveQuestionCommand removeQuestionCommand -> returnObject = handleRemoveQuestion(removeQuestionCommand);
            case FindQuestionsByTopicIdsCommand findQuestionsByTopicIdsCommand -> returnObject = handleFindQuestionsByTopicIds(findQuestionsByTopicIdsCommand);
            case UpdateQuestionTopicsCommand updateQuestionTopicsCommand -> returnObject = handleUpdateQuestionTopics(updateQuestionTopicsCommand);
            case GetQuestionByIdCommand getQuestionByIdCommand -> returnObject = handleGetQuestionById(getQuestionByIdCommand);
            case FindQuestionsByCourseAggregateIdCommand findQuestionsByCourseAggregateIdCommand -> returnObject = handleFindQuestionsByCourseAggregateId(findQuestionsByCourseAggregateIdCommand);
            case UpdateTopicCommand updateTopicCommand -> returnObject = handleUpdateTopic(updateTopicCommand);
            case RemoveTopicCommand removeTopicCommand -> returnObject = handleRemoveTopic(removeTopicCommand);
            default -> {
                logger.warning("Unknown command type: " + command.getClass().getName());
                returnObject = null;
            }
        }
        if (command.getSemanticLock() != null) {
            sagaUnitOfWorkService.registerSagaState(command.getRootAggregateId(), command.getSemanticLock(),
                    (SagaUnitOfWork) command.getUnitOfWork());
        }
        return returnObject;
    }

    private Object handleCreateQuestion(CreateQuestionCommand command) {
        logger.info("Creating question: " + command.getQuestionDto());
        try {
            QuestionDto questionDto = questionService.createQuestion(
                    command.getCourse(),
                    command.getQuestionDto(),
                    command.getTopics(),
                    command.getUnitOfWork());
            return questionDto;
        } catch (Exception e) {
            logger.severe("Failed to create question: " + e.getMessage());
            return e;
        }
    }

    private Object handleUpdateQuestion(UpdateQuestionCommand command) {
        logger.info("Updating question: " + command.getQuestionDto().getAggregateId());
        try {
            questionService.updateQuestion(
                    command.getQuestionDto(),
                    command.getUnitOfWork());
            return null;
        } catch (Exception e) {
            logger.severe("Failed to update question: " + e.getMessage());
            return e;
        }
    }

    private Object handleRemoveQuestion(RemoveQuestionCommand command) {
        logger.info("Removing question: " + command.getCourseAggregateId());
        try {
            questionService.removeQuestion(
                    command.getCourseAggregateId(),
                    command.getUnitOfWork());
            return null;
        } catch (Exception e) {
            logger.severe("Failed to remove question: " + e.getMessage());
            return e;
        }
    }

    private Object handleFindQuestionsByTopicIds(FindQuestionsByTopicIdsCommand command) {
        logger.info("Finding questions by topic IDs: " + command.getTopicIds());
        try {
            return questionService.findQuestionsByTopicIds(
                    command.getTopicIds(),
                    command.getUnitOfWork());
        } catch (Exception e) {
            logger.severe("Failed to find questions by topic IDs: " + e.getMessage());
            return e;
        }
    }

    private Object handleUpdateQuestionTopics(UpdateQuestionTopicsCommand command) {
        logger.info("Updating question topics for course: " + command.getCourseAggregateId());
        try {
            questionService.updateQuestionTopics(
                    command.getCourseAggregateId(),
                    command.getTopics(),
                    command.getUnitOfWork());
            return null;
        } catch (Exception e) {
            logger.severe("Failed to update question topics: " + e.getMessage());
            return e;
        }
    }

    private Object handleGetQuestionById(GetQuestionByIdCommand command) {
        logger.info("Getting question by ID: " + command.getAggregateId());
        try {
            return questionService.getQuestionById(
                    command.getAggregateId(),
                    command.getUnitOfWork());
        } catch (Exception e) {
            logger.severe("Failed to get question by ID: " + e.getMessage());
            return e;
        }
    }

    private Object handleFindQuestionsByCourseAggregateId(FindQuestionsByCourseAggregateIdCommand command) {
        logger.info("Finding questions by course aggregate ID: " + command.getCourseAggregateId());
        try {
            return questionService.findQuestionsByCourseAggregateId(
                    command.getCourseAggregateId(),
                    command.getUnitOfWork());
        } catch (Exception e) {
            logger.severe("Failed to find questions by course aggregate ID: " + e.getMessage());
            return e;
        }
    }

    private Object handleUpdateTopic(UpdateTopicCommand command) {
        logger.info("Updating topic in question: question=" + command.getQuestionAggregateId() + ", topic="
                + command.getTopicAggregateId());
        try {
            return questionService.updateTopic(
                    command.getQuestionAggregateId(),
                    command.getTopicAggregateId(),
                    command.getTopicName(),
                    command.getAggregateVersion(),
                    command.getUnitOfWork());
        } catch (Exception e) {
            logger.severe("Failed to update topic in question: " + e.getMessage());
            return e;
        }
    }

    private Object handleRemoveTopic(RemoveTopicCommand command) {
        logger.info("Removing topic from question: question=" + command.getQuestionAggregateId() + ", topic="
                + command.getTopicAggregateId());
        try {
            return questionService.removeTopic(
                    command.getQuestionAggregateId(),
                    command.getTopicAggregateId(),
                    command.getAggregateVersion(),
                    command.getUnitOfWork());
        } catch (Exception e) {
            logger.severe("Failed to remove topic from question: " + e.getMessage());
            return e;
        }
    }
}
