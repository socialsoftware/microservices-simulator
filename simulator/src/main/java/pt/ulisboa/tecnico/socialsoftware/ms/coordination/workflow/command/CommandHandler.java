package pt.ulisboa.tecnico.socialsoftware.ms.coordination.workflow.command;

import org.springframework.beans.factory.annotation.Autowired;
import pt.ulisboa.tecnico.socialsoftware.ms.causal.unitOfWork.CausalUnitOfWorkService;
import pt.ulisboa.tecnico.socialsoftware.ms.causal.unitOfWork.command.AbortCausalCommand;
import pt.ulisboa.tecnico.socialsoftware.ms.causal.unitOfWork.command.CommitCausalCommand;
import pt.ulisboa.tecnico.socialsoftware.ms.causal.unitOfWork.command.GetConcurrentAggregateCommand;
import pt.ulisboa.tecnico.socialsoftware.ms.causal.unitOfWork.command.PrepareCausalCommand;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.unitOfWork.SagaUnitOfWork;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.unitOfWork.SagaUnitOfWorkService;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.unitOfWork.command.AbortSagaCommand;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.unitOfWork.command.CommitSagaCommand;

import java.util.logging.Logger;


public abstract class CommandHandler {
    private static final Logger logger = Logger.getLogger(CommandHandler.class.getName());

    @Autowired(required = false)
    protected SagaUnitOfWorkService sagaUnitOfWorkService;

    @Autowired(required = false)
    protected CausalUnitOfWorkService causalUnitOfWorkService;

    protected abstract String getAggregateTypeName();

    protected abstract Object handleDomainCommand(Command command);

    public Object handle(Command command) {
        // Verify saga state
        if (command.getForbiddenStates() != null && !command.getForbiddenStates().isEmpty()) {
            sagaUnitOfWorkService.verifySagaState(command.getRootAggregateId(), command.getForbiddenStates());
        }

        Object returnObject;
        switch (command) {
            case CommitCausalCommand commitCausalCommand -> returnObject = handleCommitCausal(commitCausalCommand);
            case PrepareCausalCommand prepareCausalCommand -> returnObject = handlePrepareCausal(prepareCausalCommand);
            case AbortCausalCommand abortCausalCommand -> returnObject = handleAbortCausal(abortCausalCommand);
            case GetConcurrentAggregateCommand getConcurrentAggregateCommand ->
                returnObject = handleGetConcurrentAggregate(getConcurrentAggregateCommand);
            case CommitSagaCommand commitSagaCommand -> returnObject = handleCommitSaga(commitSagaCommand);
            case AbortSagaCommand abortSagaCommand -> returnObject = handleAbortSaga(abortSagaCommand);
            default -> returnObject = handleDomainCommand(command);
        }

        // Register saga state
        if (command.getSemanticLock() != null) {
            sagaUnitOfWorkService.registerSagaState(command.getRootAggregateId(), command.getSemanticLock(),
                    (SagaUnitOfWork) command.getUnitOfWork());
        }

        return returnObject;
    }

    protected Object handleCommitCausal(CommitCausalCommand command) {
        logger.info("Committing causal for aggregate: " + command.getRootAggregateId());
        try {
            causalUnitOfWorkService.commitCausal(command.getAggregate());
            return null;
        } catch (Exception e) {
            logger.severe("Failed to commit causal: " + e.getMessage());
            return e;
        }
    }

    protected Object handlePrepareCausal(PrepareCausalCommand command) {
        logger.info("Preparing causal for aggregate: " + command.getRootAggregateId());
        try {
            causalUnitOfWorkService.prepareCausal(command.getAggregate());
            return null;
        } catch (Exception e) {
            logger.severe("Failed to prepare causal: " + e.getMessage());
            return e;
        }
    }

    protected Object handleAbortCausal(AbortCausalCommand command) {
        logger.info("Aborting causal for aggregate: " + command.getRootAggregateId());
        try {
            causalUnitOfWorkService.abortCausal(command.getRootAggregateId());
            return null;
        } catch (Exception e) {
            logger.severe("Failed to abort causal: " + e.getMessage());
            return e;
        }
    }

    protected Object handleGetConcurrentAggregate(GetConcurrentAggregateCommand command) {
        return causalUnitOfWorkService.getConcurrentAggregate(
                command.getRootAggregateId(), command.getVersion(), getAggregateTypeName());
    }

    protected Object handleCommitSaga(CommitSagaCommand command) {
        logger.info("Committing saga for aggregate: " + command.getAggregateId());
        try {
            sagaUnitOfWorkService.commitAggregate(command.getAggregateId());
            return null;
        } catch (Exception e) {
            logger.severe("Failed to commit saga: " + e.getMessage());
            return e;
        }
    }

    protected Object handleAbortSaga(AbortSagaCommand command) {
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
