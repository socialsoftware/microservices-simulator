package pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.quiz.commandHandler;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import pt.ulisboa.tecnico.socialsoftware.ms.causal.unitOfWork.CausalUnitOfWorkService;
import pt.ulisboa.tecnico.socialsoftware.ms.causal.unitOfWork.command.CommitCausalCommand;
import pt.ulisboa.tecnico.socialsoftware.ms.coordination.workflow.Command;
import pt.ulisboa.tecnico.socialsoftware.ms.coordination.workflow.CommandHandler;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.unitOfWork.SagaUnitOfWork;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.unitOfWork.SagaUnitOfWorkService;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.unitOfWork.command.AbortSagaCommand;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.unitOfWork.command.CommitSagaCommand;
import pt.ulisboa.tecnico.socialsoftware.quizzes.command.quiz.*;
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.quiz.service.QuizService;

import java.util.logging.Logger;

@Component
public class QuizCommandHandler implements CommandHandler {
    private static final Logger logger = Logger.getLogger(QuizCommandHandler.class.getName());

    @Autowired
    private QuizService quizService;

    @Autowired(required = false)
    private SagaUnitOfWorkService sagaUnitOfWorkService;

    @Autowired(required = false)
    private CausalUnitOfWorkService causalUnitOfWorkService;

    @Override
    public Object handle(Command command) {
        if (command.getForbiddenStates() != null && !command.getForbiddenStates().isEmpty()) {
            sagaUnitOfWorkService.verifySagaState(command.getRootAggregateId(), command.getForbiddenStates());
        }
        Object returnObject;
        switch (command) {
            case StartTournamentQuizCommand startTournamentQuizCommand ->
                returnObject = handleStartTournamentQuiz(startTournamentQuizCommand);
            case GetQuizByIdCommand getQuizByIdCommand -> returnObject = handleGetQuizById(getQuizByIdCommand);
            case GenerateQuizCommand generateQuizCommand -> returnObject = handleGenerateQuiz(generateQuizCommand);
            case CreateQuizCommand createQuizCommand -> returnObject = handleCreateQuiz(createQuizCommand);
            case UpdateGeneratedQuizCommand updateGeneratedQuizCommand ->
                returnObject = handleUpdateGeneratedQuiz(updateGeneratedQuizCommand);
            case UpdateQuizCommand updateQuizCommand -> returnObject = handleUpdateQuiz(updateQuizCommand);
            case GetAvailableQuizzesCommand getAvailableQuizzesCommand ->
                returnObject = handleGetAvailableQuizzes(getAvailableQuizzesCommand);
            case RemoveCourseExecutionCommand removeCourseExecutionCommand ->
                returnObject = handleRemoveCourseExecution(removeCourseExecutionCommand);
            case UpdateQuestionCommand updateQuestionCommand ->
                returnObject = handleUpdateQuestion(updateQuestionCommand);
            case RemoveQuizQuestionCommand removeQuizQuestionCommand ->
                returnObject = handleRemoveQuizQuestion(removeQuizQuestionCommand);
            case RemoveQuizCommand removeQuizCommand -> returnObject = handleRemoveQuiz(removeQuizCommand);
            case CommitCausalCommand commitCausalCommand -> returnObject = handleCommitCausal(commitCausalCommand);
            case CommitSagaCommand commitSagaCommand -> returnObject = handleCommitSaga(commitSagaCommand);
            case AbortSagaCommand abortSagaCommand -> returnObject = handleAbortSaga(abortSagaCommand);
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
        logger.info("Generating quiz for course execution: " + command.getCourseExecutionDto().getAggregateId());
        try {
            return quizService.generateQuiz(
                    command.getCourseExecutionDto(),
                    command.getQuizDto(),
                    command.getQuestionDtos(),
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
                    command.getQuestionDtos(),
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

    private Object handleCommitCausal(CommitCausalCommand command) {
        logger.info("Committing causal for aggregate: " + command.getRootAggregateId());
        try {
            causalUnitOfWorkService.commitCausal(command.getAggregate());
            return null;
        } catch (Exception e) {
            logger.severe("Failed to commit causal: " + e.getMessage());
            return e;
        }
    }

    private Object handleCommitSaga(CommitSagaCommand command) {
        logger.info("Committing saga for aggregate: " + command.getAggregateId());
        try {
            sagaUnitOfWorkService.commitAggregate(command.getAggregateId());
            return null;
        } catch (Exception e) {
            logger.severe("Failed to commit saga: " + e.getMessage());
            return e;
        }
    }

    private Object handleAbortSaga(AbortSagaCommand command) {
        logger.info("Aborting saga for aggregate: " + command.getAggregateId());
        try {
            sagaUnitOfWorkService.abortAggregate(command.getAggregateId(), command.getPreviousState());
            return null;
        } catch (Exception e) {
            logger.severe("Failed to abort saga: " + e.getMessage());
            return e;
        }
    }
}
