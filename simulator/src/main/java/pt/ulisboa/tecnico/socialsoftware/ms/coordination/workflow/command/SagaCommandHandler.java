package pt.ulisboa.tecnico.socialsoftware.ms.coordination.workflow.command;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.unitOfWork.SagaUnitOfWork;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.unitOfWork.SagaUnitOfWorkService;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.unitOfWork.command.AbortSagaCommand;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.unitOfWork.command.CommitSagaCommand;

import java.util.logging.Logger;

@Component
@Profile("sagas")
public class SagaCommandHandler {
    private static final Logger logger = Logger.getLogger(SagaCommandHandler.class.getName());

    @Autowired
    private SagaUnitOfWorkService sagaUnitOfWorkService;

    public Object handle(Command command, CommandHandler serviceCommandHandler) {
        if (command.getForbiddenStates() != null && !command.getForbiddenStates().isEmpty()) {
            sagaUnitOfWorkService.verifySagaState(command.getRootAggregateId(), command.getForbiddenStates());
        }

        Object returnObject = switch (command) {
            case CommitSagaCommand commitSagaCommand -> handleCommitSaga(commitSagaCommand);
            case AbortSagaCommand abortSagaCommand -> handleAbortSaga(abortSagaCommand);
            default -> serviceCommandHandler.handleDomainCommand(command);
        };

        if (command.getSemanticLock() != null) {
            sagaUnitOfWorkService.registerSagaState(
                    command.getRootAggregateId(),
                    command.getSemanticLock(),
                    (SagaUnitOfWork) command.getUnitOfWork());
        }

        return returnObject;
    }

    private Object handleCommitSaga(CommitSagaCommand command) {
        logger.info("Committing saga for aggregate: " + command.getAggregateId());
        sagaUnitOfWorkService.commitAggregate(command.getAggregateId());
        return null;
    }

    private Object handleAbortSaga(AbortSagaCommand command) {
        logger.info("Aborting saga for aggregate: " + command.getAggregateId());
        sagaUnitOfWorkService.abortAggregate(command.getAggregateId(), command.getPreviousState());
        return null;
    }
}
