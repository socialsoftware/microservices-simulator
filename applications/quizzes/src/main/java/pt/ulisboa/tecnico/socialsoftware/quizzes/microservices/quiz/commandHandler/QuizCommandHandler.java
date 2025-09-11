package pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.quiz.commandHandler;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import pt.ulisboa.tecnico.socialsoftware.ms.coordination.workflow.Command;
import pt.ulisboa.tecnico.socialsoftware.ms.coordination.workflow.CommandHandler;
import pt.ulisboa.tecnico.socialsoftware.quizzes.command.quiz.*;
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.quiz.service.QuizService;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.unitOfWork.SagaUnitOfWork;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.unitOfWork.SagaUnitOfWorkService;

import java.util.logging.Logger;

@Component
public class QuizCommandHandler implements CommandHandler {
    private static final Logger logger = Logger.getLogger(QuizCommandHandler.class.getName());

    @Autowired
    private QuizService quizService;

    @Autowired(required = false)
    private SagaUnitOfWorkService sagaUnitOfWorkService;

    @Override
    public Object handle(Command command) {
        if (command.getForbiddenStates() != null && !command.getForbiddenStates().isEmpty()) {
            sagaUnitOfWorkService.verifySagaState(command.getRootAggregateId(), command.getForbiddenStates());
        }
        Object returnObject;
        if (command instanceof StartTournamentQuizCommand) {
            returnObject = handleStartTournamentQuiz((StartTournamentQuizCommand) command);
        } else if (command instanceof GetQuizByIdCommand) {
            returnObject = handleGetQuizById((GetQuizByIdCommand) command);
        } else if (command instanceof GenerateQuizCommand) {
            returnObject = handleGenerateQuiz((GenerateQuizCommand) command);
        } else if (command instanceof CreateQuizCommand) {
            returnObject = handleCreateQuiz((CreateQuizCommand) command);
        } else if (command instanceof UpdateGeneratedQuizCommand) {
            returnObject = handleUpdateGeneratedQuiz((UpdateGeneratedQuizCommand) command);
        } else if (command instanceof UpdateQuizCommand) {
            returnObject = handleUpdateQuiz((UpdateQuizCommand) command);
        } else if (command instanceof GetAvailableQuizzesCommand) {
            returnObject = handleGetAvailableQuizzes((GetAvailableQuizzesCommand) command);
        } else if (command instanceof RemoveCourseExecutionCommand) {
            returnObject = handleRemoveCourseExecution((RemoveCourseExecutionCommand) command);
        } else if (command instanceof UpdateQuestionCommand) {
            returnObject = handleUpdateQuestion((UpdateQuestionCommand) command);
        } else if (command instanceof RemoveQuizQuestionCommand) {
            returnObject = handleRemoveQuizQuestion((RemoveQuizQuestionCommand) command);
        } else if (command instanceof RemoveQuizCommand) {
            returnObject = handleRemoveQuiz((RemoveQuizCommand) command);
        } else {
            logger.warning("Unknown command type: " + command.getClass().getName());
            returnObject = null;
        }
        if (command.getSemanticLock() != null) {
            sagaUnitOfWorkService.registerSagaState(command.getRootAggregateId(), command.getSemanticLock(),
                    (SagaUnitOfWork) command.getUnitOfWork());
        }
        return returnObject;
    }

    private Object handleStartTournamentQuiz(StartTournamentQuizCommand command) {
        logger.info("Starting tournament quiz: " + command.getQuizAggregateId());
        try {
            return quizService.startTournamentQuiz(
                    command.getUserAggregateId(),
                    command.getQuizAggregateId(),
                    command.getUnitOfWork());
        } catch (Exception e) {
            logger.severe("Failed to start tournament quiz: " + e.getMessage());
            return e;
        }
    }

    private Object handleGetQuizById(GetQuizByIdCommand command) {
        logger.info("Getting quiz by ID: " + command.getAggregateId());
        try {
            return quizService.getQuizById(
                    command.getAggregateId(),
                    command.getUnitOfWork());
        } catch (Exception e) {
            logger.severe("Failed to get quiz by ID: " + e.getMessage());
            return e;
        }
    }

    private Object handleGenerateQuiz(GenerateQuizCommand command) {
        logger.info("Generating quiz for course execution: " + command.getCourseExecutionAggregateId());
        try {
            return quizService.generateQuiz(
                    command.getCourseExecutionAggregateId(),
                    command.getQuizDto(),
                    command.getTopicIds(),
                    command.getNumberOfQuestions(),
                    command.getUnitOfWork());
        } catch (Exception e) {
            logger.severe("Failed to generate quiz: " + e.getMessage());
            return e;
        }
    }

    private Object handleCreateQuiz(CreateQuizCommand command) {
        logger.info("Creating quiz for course execution: " + command.getQuizCourseExecution());
        try {
            return quizService.createQuiz(
                    command.getQuizCourseExecution(),
                    command.getQuestions(),
                    command.getQuizDto(),
                    command.getUnitOfWork());
        } catch (Exception e) {
            logger.severe("Failed to create quiz: " + e.getMessage());
            return e;
        }
    }

    private Object handleUpdateGeneratedQuiz(UpdateGeneratedQuizCommand command) {
        logger.info("Updating generated quiz: " + command.getQuizDto().getAggregateId());
        try {
            return quizService.updateGeneratedQuiz(
                    command.getQuizDto(),
                    command.getTopicsAggregateIds(),
                    command.getNumberOfQuestions(),
                    command.getUnitOfWork());
        } catch (Exception e) {
            logger.severe("Failed to update generated quiz: " + e.getMessage());
            return e;
        }
    }

    private Object handleUpdateQuiz(UpdateQuizCommand command) {
        logger.info("Updating quiz: " + command.getQuizDto().getAggregateId());
        try {
            return quizService.updateQuiz(
                    command.getQuizDto(),
                    command.getQuizQuestions(),
                    command.getUnitOfWork());
        } catch (Exception e) {
            logger.severe("Failed to update quiz: " + e.getMessage());
            return e;
        }
    }

    private Object handleGetAvailableQuizzes(GetAvailableQuizzesCommand command) {
        logger.info("Getting available quizzes for course execution: " + command.getCourseExecutionAggregateId());
        try {
            return quizService.getAvailableQuizzes(
                    command.getCourseExecutionAggregateId(),
                    command.getUnitOfWork());
        } catch (Exception e) {
            logger.severe("Failed to get available quizzes: " + e.getMessage());
            return e;
        }
    }

    private Object handleRemoveQuiz(RemoveQuizCommand command) {
        logger.info("Removing quiz: " + command.getQuizAggregateId());
        try {
            quizService.removeQuiz(
                    command.getQuizAggregateId(),
                    command.getUnitOfWork());
            return null;
        } catch (Exception e) {
            logger.severe("Failed to remove quiz: " + e.getMessage());
            return e;
        }
    }

    private Object handleRemoveCourseExecution(RemoveCourseExecutionCommand command) {
        logger.info("Removing course execution from quiz: " + command.getQuizAggregateId());
        try {
            return quizService.removeCourseExecution(
                    command.getQuizAggregateId(),
                    command.getCourseExecutionId(),
                    command.getAggregateVersion(),
                    command.getUnitOfWork());
        } catch (Exception e) {
            logger.severe("Failed to remove course execution from quiz: " + e.getMessage());
            return e;
        }
    }

    private Object handleUpdateQuestion(UpdateQuestionCommand command) {
        logger.info("Updating quiz question in quiz: " + command.getQuizAggregateId());
        try {
            quizService.updateQuestion(
                    command.getQuizAggregateId(),
                    command.getQuestionAggregateId(),
                    command.getTitle(),
                    command.getContent(),
                    command.getAggregateVersion(),
                    command.getUnitOfWork());
            return null;
        } catch (Exception e) {
            logger.severe("Failed to update quiz question: " + e.getMessage());
            return e;
        }
    }

    private Object handleRemoveQuizQuestion(RemoveQuizQuestionCommand command) {
        logger.info("Removing question from quiz: " + command.getQuizAggregateId());
        try {
            quizService.removeQuizQuestion(
                    command.getQuizAggregateId(),
                    command.getQuestionAggregateId(),
                    command.getUnitOfWork());
            return null;
        } catch (Exception e) {
            logger.severe("Failed to remove quiz question: " + e.getMessage());
            return e;
        }
    }
}
