package pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.question.commandHandler;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import pt.ulisboa.tecnico.socialsoftware.ms.coordination.workflow.command.Command;
import pt.ulisboa.tecnico.socialsoftware.ms.coordination.workflow.command.CommandHandler;
import pt.ulisboa.tecnico.socialsoftware.quizzes.command.question.*;
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.question.service.QuestionService;

import java.util.logging.Logger;

@Component
public class QuestionCommandHandler extends CommandHandler {
    private static final Logger logger = Logger.getLogger(QuestionCommandHandler.class.getName());

    @Autowired
    private QuestionService questionService;

    @Override
    protected String getAggregateTypeName() {
        return "Question";
    }

    @Override
    protected Object handleDomainCommand(Command command) {
        return switch (command) {
            case CreateQuestionCommand cmd -> handleCreateQuestion(cmd);
            case UpdateQuestionCommand cmd -> handleUpdateQuestion(cmd);
            case RemoveQuestionCommand cmd -> handleRemoveQuestion(cmd);
            case FindQuestionsByTopicIdsCommand cmd -> handleFindQuestionsByTopicIds(cmd);
            case UpdateQuestionTopicsCommand cmd -> handleUpdateQuestionTopics(cmd);
            case GetQuestionByIdCommand cmd -> handleGetQuestionById(cmd);
            case FindQuestionsByCourseAggregateIdCommand cmd -> handleFindQuestionsByCourseAggregateId(cmd);
            case UpdateTopicCommand cmd -> handleUpdateTopic(cmd);
            case RemoveTopicCommand cmd -> handleRemoveTopic(cmd);
            default -> {
                logger.warning("Unknown command type: " + command.getClass().getName());
                yield null;
            }
        };
    }

    private Object handleCreateQuestion(CreateQuestionCommand command) {
        logger.info("Creating question: " + command.getQuestionDto());
        return questionService.createQuestion(
                command.getCourse(),
                command.getQuestionDto(),
                command.getTopics(),
                command.getUnitOfWork());
    }

    private Object handleUpdateQuestion(UpdateQuestionCommand command) {
        logger.info("Updating question: " + command.getQuestionDto().getAggregateId());
        questionService.updateQuestion(
                command.getQuestionDto(),
                command.getUnitOfWork());
        return null;
    }

    private Object handleRemoveQuestion(RemoveQuestionCommand command) {
        logger.info("Removing question: " + command.getCourseAggregateId());
        questionService.removeQuestion(
                command.getCourseAggregateId(),
                command.getUnitOfWork());
        return null;
    }

    private Object handleFindQuestionsByTopicIds(FindQuestionsByTopicIdsCommand command) {
        logger.info("Finding questions by topic IDs: " + command.getTopicIds());
        return questionService.findQuestionsByTopicIds(
                command.getTopicIds(),
                command.getUnitOfWork());
    }

    private Object handleUpdateQuestionTopics(UpdateQuestionTopicsCommand command) {
        logger.info("Updating question topics for course: " + command.getCourseAggregateId());
        questionService.updateQuestionTopics(
                command.getCourseAggregateId(),
                command.getTopics(),
                command.getUnitOfWork());
        return null;
    }

    private Object handleGetQuestionById(GetQuestionByIdCommand command) {
        logger.info("Getting question by ID: " + command.getAggregateId());
        return questionService.getQuestionById(
                command.getAggregateId(),
                command.getUnitOfWork());
    }

    private Object handleFindQuestionsByCourseAggregateId(FindQuestionsByCourseAggregateIdCommand command) {
        logger.info("Finding questions by course aggregate ID: " + command.getCourseAggregateId());
        return questionService.findQuestionsByCourseAggregateId(
                command.getCourseAggregateId(),
                command.getUnitOfWork());
    }

    private Object handleUpdateTopic(UpdateTopicCommand command) {
        logger.info("Updating topic in question: question=" + command.getQuestionAggregateId() + ", topic="
                + command.getTopicAggregateId());
        return questionService.updateTopic(
                command.getQuestionAggregateId(),
                command.getTopicAggregateId(),
                command.getTopicName(),
                command.getAggregateVersion(),
                command.getUnitOfWork());
    }

    private Object handleRemoveTopic(RemoveTopicCommand command) {
        logger.info("Removing topic from question: question=" + command.getQuestionAggregateId() + ", topic="
                + command.getTopicAggregateId());
        return questionService.removeTopic(
                command.getQuestionAggregateId(),
                command.getTopicAggregateId(),
                command.getAggregateVersion(),
                command.getUnitOfWork());
    }
}
