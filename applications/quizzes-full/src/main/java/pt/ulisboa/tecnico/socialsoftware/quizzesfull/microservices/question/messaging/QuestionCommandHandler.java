package pt.ulisboa.tecnico.socialsoftware.quizzesfull.microservices.question.messaging;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import pt.ulisboa.tecnico.socialsoftware.ms.messaging.Command;
import pt.ulisboa.tecnico.socialsoftware.ms.messaging.CommandHandler;
import pt.ulisboa.tecnico.socialsoftware.quizzesfull.commands.question.CreateQuestionCommand;
import pt.ulisboa.tecnico.socialsoftware.quizzesfull.commands.question.DeleteQuestionCommand;
import pt.ulisboa.tecnico.socialsoftware.quizzesfull.commands.question.GetQuestionByIdCommand;
import pt.ulisboa.tecnico.socialsoftware.quizzesfull.commands.question.GetQuestionsByCourseExecutionIdCommand;
import pt.ulisboa.tecnico.socialsoftware.quizzesfull.commands.question.UpdateQuestionCommand;
import pt.ulisboa.tecnico.socialsoftware.quizzesfull.microservices.question.service.QuestionService;

import java.util.logging.Logger;

@Component
public class QuestionCommandHandler extends CommandHandler {
    private static final Logger logger = Logger.getLogger(QuestionCommandHandler.class.getName());

    @Autowired
    private QuestionService questionService;

    @Override
    public String getAggregateTypeName() {
        return "Question";
    }

    @Override
    public Object handleDomainCommand(Command command) {
        return switch (command) {
            case GetQuestionByIdCommand cmd -> questionService.getQuestionById(
                    cmd.getQuestionAggregateId(), cmd.getUnitOfWork());
            case GetQuestionsByCourseExecutionIdCommand cmd -> questionService.getQuestionsByCourseExecutionId(
                    cmd.getCourseAggregateId(), cmd.getUnitOfWork());
            case CreateQuestionCommand cmd -> questionService.createQuestion(
                    cmd.getTitle(), cmd.getContent(), cmd.getQuestionCourse(),
                    cmd.getOptions(), cmd.getTopics(), cmd.getUnitOfWork());
            case UpdateQuestionCommand cmd -> {
                questionService.updateQuestion(
                        cmd.getQuestionAggregateId(), cmd.getTitle(), cmd.getContent(),
                        cmd.getTopics(), cmd.getUnitOfWork());
                yield null;
            }
            case DeleteQuestionCommand cmd -> {
                questionService.deleteQuestion(cmd.getQuestionAggregateId(), cmd.getUnitOfWork());
                yield null;
            }
            default -> {
                logger.warning("Unknown command type: " + command.getClass().getName());
                yield null;
            }
        };
    }
}
