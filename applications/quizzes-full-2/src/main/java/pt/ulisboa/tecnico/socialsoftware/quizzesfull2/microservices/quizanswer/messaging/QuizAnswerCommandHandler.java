package pt.ulisboa.tecnico.socialsoftware.quizzesfull2.microservices.quizanswer.messaging;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import pt.ulisboa.tecnico.socialsoftware.ms.messaging.Command;
import pt.ulisboa.tecnico.socialsoftware.ms.messaging.CommandHandler;
import pt.ulisboa.tecnico.socialsoftware.quizzesfull2.commands.quizanswer.AnswerQuestionCommand;
import pt.ulisboa.tecnico.socialsoftware.quizzesfull2.commands.quizanswer.ConcludeQuizCommand;
import pt.ulisboa.tecnico.socialsoftware.quizzesfull2.commands.quizanswer.CreateQuizAnswerCommand;
import pt.ulisboa.tecnico.socialsoftware.quizzesfull2.commands.quizanswer.GetQuizAnswerByIdCommand;
import pt.ulisboa.tecnico.socialsoftware.quizzesfull2.commands.quizanswer.GetQuizAnswerForStudentAndQuizCommand;
import pt.ulisboa.tecnico.socialsoftware.quizzesfull2.microservices.quizanswer.service.QuizAnswerService;

import java.util.logging.Logger;

@Component
public class QuizAnswerCommandHandler extends CommandHandler {
    private static final Logger logger = Logger.getLogger(QuizAnswerCommandHandler.class.getName());

    @Autowired
    private QuizAnswerService quizAnswerService;

    @Override
    public String getAggregateTypeName() {
        return "QuizAnswer";
    }

    @Override
    public Object handleDomainCommand(Command command) {
        return switch (command) {
            case GetQuizAnswerByIdCommand cmd -> handleGetQuizAnswerById(cmd);
            case GetQuizAnswerForStudentAndQuizCommand cmd -> handleGetQuizAnswerForStudentAndQuiz(cmd);
            case CreateQuizAnswerCommand cmd -> handleCreateQuizAnswer(cmd);
            case AnswerQuestionCommand cmd -> {
                handleAnswerQuestion(cmd);
                yield null;
            }
            case ConcludeQuizCommand cmd -> {
                handleConcludeQuiz(cmd);
                yield null;
            }
            default -> {
                logger.warning("Unknown command: " + command.getClass().getName());
                yield null;
            }
        };
    }

    private Object handleGetQuizAnswerById(GetQuizAnswerByIdCommand command) {
        return quizAnswerService.getQuizAnswerById(command.getQuizAnswerAggregateId(), command.getUnitOfWork());
    }

    private Object handleGetQuizAnswerForStudentAndQuiz(GetQuizAnswerForStudentAndQuizCommand command) {
        return quizAnswerService.getQuizAnswerForStudentAndQuiz(command.getUserAggregateId(),
                command.getQuizAggregateId(), command.getUnitOfWork());
    }

    private Object handleCreateQuizAnswer(CreateQuizAnswerCommand command) {
        return quizAnswerService.createQuizAnswer(command.getQuizDto(), command.getUserDto(),
                command.getExecutionDto(), command.getQuestionAnswers(), command.getUnitOfWork());
    }

    private void handleAnswerQuestion(AnswerQuestionCommand command) {
        quizAnswerService.answerQuestion(command.getQuizAnswerAggregateId(), command.getQuestionAggregateId(),
                command.getOptionSequenceChoice(), command.getOptionKey(), command.getTimeTaken(),
                command.getUnitOfWork());
    }

    private void handleConcludeQuiz(ConcludeQuizCommand command) {
        quizAnswerService.concludeQuiz(command.getQuizAnswerAggregateId(), command.getUnitOfWork());
    }
}
