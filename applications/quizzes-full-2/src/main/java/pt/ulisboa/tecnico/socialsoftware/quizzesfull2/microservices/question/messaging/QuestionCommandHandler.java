package pt.ulisboa.tecnico.socialsoftware.quizzesfull2.microservices.question.messaging;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import pt.ulisboa.tecnico.socialsoftware.ms.messaging.Command;
import pt.ulisboa.tecnico.socialsoftware.ms.messaging.CommandHandler;
import pt.ulisboa.tecnico.socialsoftware.quizzesfull2.commands.question.GetQuestionByIdCommand;
import pt.ulisboa.tecnico.socialsoftware.quizzesfull2.commands.question.GetQuestionsByCourseCommand;
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
}
