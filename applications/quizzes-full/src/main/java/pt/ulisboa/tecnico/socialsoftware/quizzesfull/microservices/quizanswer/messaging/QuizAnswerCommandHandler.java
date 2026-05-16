package pt.ulisboa.tecnico.socialsoftware.quizzesfull.microservices.quizanswer.messaging;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import pt.ulisboa.tecnico.socialsoftware.ms.messaging.Command;
import pt.ulisboa.tecnico.socialsoftware.ms.messaging.CommandHandler;
import pt.ulisboa.tecnico.socialsoftware.quizzesfull.commands.quizanswer.AnswerQuestionCommand;
import pt.ulisboa.tecnico.socialsoftware.quizzesfull.commands.quizanswer.ConcludeQuizCommand;
import pt.ulisboa.tecnico.socialsoftware.quizzesfull.commands.quizanswer.CreateQuizAnswerCommand;
import pt.ulisboa.tecnico.socialsoftware.quizzesfull.commands.quizanswer.GetQuizAnswerByIdCommand;
import pt.ulisboa.tecnico.socialsoftware.quizzesfull.microservices.quizanswer.service.QuizAnswerService;

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
            case GetQuizAnswerByIdCommand cmd -> quizAnswerService.getQuizAnswerById(
                    cmd.getQuizAnswerAggregateId(), cmd.getUnitOfWork());
            case CreateQuizAnswerCommand cmd -> quizAnswerService.createQuizAnswer(
                    cmd.getQuizAggregateId(), cmd.getQuizVersion(),
                    cmd.getUserAggregateId(), cmd.getUserVersion(),
                    cmd.getUserName(), cmd.getUserUsername(),
                    cmd.getExecutionAggregateId(), cmd.getExecutionVersion(),
                    cmd.getUnitOfWork());
            case AnswerQuestionCommand cmd -> {
                quizAnswerService.answerQuestion(
                        cmd.getQuizAnswerAggregateId(), cmd.getQuestionAggregateId(),
                        cmd.getQuestionVersion(), cmd.getOptionKey(), cmd.getTimeTaken(),
                        cmd.getUnitOfWork());
                yield null;
            }
            case ConcludeQuizCommand cmd -> {
                quizAnswerService.concludeQuiz(cmd.getQuizAnswerAggregateId(), cmd.getUnitOfWork());
                yield null;
            }
            default -> {
                logger.warning("Unknown command type: " + command.getClass().getName());
                yield null;
            }
        };
    }
}
