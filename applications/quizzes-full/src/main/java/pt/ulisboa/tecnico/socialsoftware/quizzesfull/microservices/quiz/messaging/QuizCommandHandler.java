package pt.ulisboa.tecnico.socialsoftware.quizzesfull.microservices.quiz.messaging;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import pt.ulisboa.tecnico.socialsoftware.ms.messaging.Command;
import pt.ulisboa.tecnico.socialsoftware.ms.messaging.CommandHandler;
import pt.ulisboa.tecnico.socialsoftware.quizzesfull.commands.quiz.CreateQuizCommand;
import pt.ulisboa.tecnico.socialsoftware.quizzesfull.commands.quiz.GetQuizByIdCommand;
import pt.ulisboa.tecnico.socialsoftware.quizzesfull.commands.quiz.UpdateQuizCommand;
import pt.ulisboa.tecnico.socialsoftware.quizzesfull.microservices.quiz.service.QuizService;

import java.util.logging.Logger;

@Component
public class QuizCommandHandler extends CommandHandler {
    private static final Logger logger = Logger.getLogger(QuizCommandHandler.class.getName());

    @Autowired
    private QuizService quizService;

    @Override
    public String getAggregateTypeName() {
        return "Quiz";
    }

    @Override
    public Object handleDomainCommand(Command command) {
        return switch (command) {
            case GetQuizByIdCommand cmd -> quizService.getQuizById(
                    cmd.getQuizAggregateId(), cmd.getUnitOfWork());
            case CreateQuizCommand cmd -> quizService.createQuiz(
                    cmd.getTitle(), cmd.getAvailableDate(), cmd.getConclusionDate(),
                    cmd.getResultsDate(), cmd.getQuizType(),
                    cmd.getQuizExecution(), cmd.getQuestions(), cmd.getUnitOfWork());
            case UpdateQuizCommand cmd -> {
                quizService.updateQuiz(
                        cmd.getQuizAggregateId(), cmd.getAvailableDate(), cmd.getConclusionDate(),
                        cmd.getResultsDate(), cmd.getQuestions(), cmd.getUnitOfWork());
                yield null;
            }
            default -> {
                logger.warning("Unknown command type: " + command.getClass().getName());
                yield null;
            }
        };
    }
}
