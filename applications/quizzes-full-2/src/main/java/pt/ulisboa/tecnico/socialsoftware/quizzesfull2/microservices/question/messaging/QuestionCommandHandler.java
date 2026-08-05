package pt.ulisboa.tecnico.socialsoftware.quizzesfull2.microservices.question.messaging;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import pt.ulisboa.tecnico.socialsoftware.ms.messaging.Command;
import pt.ulisboa.tecnico.socialsoftware.ms.messaging.CommandHandler;
import pt.ulisboa.tecnico.socialsoftware.quizzesfull2.commands.question.CreateQuestionCommand;
import pt.ulisboa.tecnico.socialsoftware.quizzesfull2.commands.question.DeleteQuestionCommand;
import pt.ulisboa.tecnico.socialsoftware.quizzesfull2.commands.question.GetQuestionByIdCommand;
import pt.ulisboa.tecnico.socialsoftware.quizzesfull2.commands.question.GetQuestionsByCourseCommand;
import pt.ulisboa.tecnico.socialsoftware.quizzesfull2.commands.question.UpdateQuestionCommand;
import pt.ulisboa.tecnico.socialsoftware.quizzesfull2.microservices.question.service.QuestionService;

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
            case GetQuestionByIdCommand cmd -> handleGetQuestionById(cmd);
            case GetQuestionsByCourseCommand cmd -> handleGetQuestionsByCourse(cmd);
            case CreateQuestionCommand cmd -> handleCreateQuestion(cmd);
            case UpdateQuestionCommand cmd -> {
                handleUpdateQuestion(cmd);
                yield null;
            }
            case DeleteQuestionCommand cmd -> {
                handleDeleteQuestion(cmd);
                yield null;
            }
            default -> {
                logger.warning("Unknown command: " + command.getClass().getName());
                yield null;
            }
        };
    }

    private Object handleGetQuestionById(GetQuestionByIdCommand command) {
        return questionService.getQuestionById(command.getQuestionAggregateId(), command.getUnitOfWork());
    }

    private Object handleGetQuestionsByCourse(GetQuestionsByCourseCommand command) {
        return questionService.getQuestionsByCourse(command.getCourseAggregateId(), command.getUnitOfWork());
    }

    private Object handleCreateQuestion(CreateQuestionCommand command) {
        return questionService.createQuestion(command.getQuestionDto(), command.getTopics(),
                command.getUnitOfWork());
    }

    private void handleUpdateQuestion(UpdateQuestionCommand command) {
        questionService.updateQuestion(command.getQuestionAggregateId(), command.getTitle(),
                command.getContent(), command.getTopics(), command.getUnitOfWork());
    }

    private void handleDeleteQuestion(DeleteQuestionCommand command) {
        questionService.deleteQuestion(command.getQuestionAggregateId(), command.getUnitOfWork());
    }
}
