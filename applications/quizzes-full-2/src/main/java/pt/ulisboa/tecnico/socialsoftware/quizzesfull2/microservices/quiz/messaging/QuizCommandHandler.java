package pt.ulisboa.tecnico.socialsoftware.quizzesfull2.microservices.quiz.messaging;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import pt.ulisboa.tecnico.socialsoftware.ms.messaging.Command;
import pt.ulisboa.tecnico.socialsoftware.ms.messaging.CommandHandler;
import pt.ulisboa.tecnico.socialsoftware.quizzesfull2.commands.quiz.CreateQuizCommand;
import pt.ulisboa.tecnico.socialsoftware.quizzesfull2.commands.quiz.GetQuizByIdCommand;
import pt.ulisboa.tecnico.socialsoftware.quizzesfull2.commands.quiz.GetQuizzesForExecutionCommand;
import pt.ulisboa.tecnico.socialsoftware.quizzesfull2.commands.quiz.UpdateQuizCommand;
import pt.ulisboa.tecnico.socialsoftware.quizzesfull2.microservices.quiz.service.QuizService;

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
            case GetQuizByIdCommand cmd -> handleGetQuizById(cmd);
            case GetQuizzesForExecutionCommand cmd -> handleGetQuizzesForExecution(cmd);
            case CreateQuizCommand cmd -> handleCreateQuiz(cmd);
            case UpdateQuizCommand cmd -> {
                handleUpdateQuiz(cmd);
                yield null;
            }
            default -> {
                logger.warning("Unknown command: " + command.getClass().getName());
                yield null;
            }
        };
    }

    private Object handleGetQuizById(GetQuizByIdCommand command) {
        return quizService.getQuizById(command.getQuizAggregateId(), command.getUnitOfWork());
    }

    private Object handleGetQuizzesForExecution(GetQuizzesForExecutionCommand command) {
        return quizService.getQuizzesForExecution(command.getExecutionAggregateId(), command.getUnitOfWork());
    }

    private Object handleCreateQuiz(CreateQuizCommand command) {
        return quizService.createQuiz(command.getQuizDto(), command.getExecutionDto(), command.getQuestions(),
                command.getUnitOfWork());
    }

    private void handleUpdateQuiz(UpdateQuizCommand command) {
        quizService.updateQuiz(command.getQuizAggregateId(), command.getTitle(), command.getAvailableDate(),
                command.getConclusionDate(), command.getResultsDate(), command.getQuestions(),
                command.getUnitOfWork());
    }
}
