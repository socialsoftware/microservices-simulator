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
public class SagaCommandHandler implements TransactionCommandHandler {
    private static final Logger logger = Logger.getLogger(SagaCommandHandler.class.getName());

    @SuppressWarnings("SpringJavaInjectionPointsAutowiringInspection")
    @Autowired
    private SagaUnitOfWorkService sagaUnitOfWorkService;

    public Object handle(Command command, CommandHandler serviceCommandHandler) {
        if (command instanceof SagaCommand sagaCommand) {
            if (sagaCommand.getForbiddenStates() != null && !sagaCommand.getForbiddenStates().isEmpty()) {
                sagaUnitOfWorkService.verifySagaState(sagaCommand.getRootAggregateId(), sagaCommand.getForbiddenStates());
            }
        }

        Object returnObject = switch (command) {
            case CommitSagaCommand commitSagaCommand -> handleCommitSaga(commitSagaCommand);
            case AbortSagaCommand abortSagaCommand -> handleAbortSaga(abortSagaCommand);
            case SagaCommand sagaCommand -> serviceCommandHandler.handleDomainCommand(sagaCommand.getPayload());
            default -> serviceCommandHandler.handleDomainCommand(command);
        };

        if (command instanceof SagaCommand sagaCommand && sagaCommand.getSemanticLock() != null) {
            sagaUnitOfWorkService.registerSagaState(
                    sagaCommand.getRootAggregateId(),
                    sagaCommand.getSemanticLock(),
                    (SagaUnitOfWork) sagaCommand.getUnitOfWork());
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
