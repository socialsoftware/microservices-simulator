package pt.ulisboa.tecnico.socialsoftware.ms.coordination.workflow.command;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import pt.ulisboa.tecnico.socialsoftware.ms.causal.unitOfWork.CausalUnitOfWorkService;
import pt.ulisboa.tecnico.socialsoftware.ms.causal.unitOfWork.command.AbortCausalCommand;
import pt.ulisboa.tecnico.socialsoftware.ms.causal.unitOfWork.command.CommitCausalCommand;
import pt.ulisboa.tecnico.socialsoftware.ms.causal.unitOfWork.command.GetConcurrentAggregateCommand;
import pt.ulisboa.tecnico.socialsoftware.ms.causal.unitOfWork.command.PrepareCausalCommand;

import java.util.logging.Logger;

@Component
@Profile("tcc")
public class CausalCommandHandler implements TransactionCommandHandler {
    private static final Logger logger = Logger.getLogger(CausalCommandHandler.class.getName());

    @SuppressWarnings("SpringJavaInjectionPointsAutowiringInspection")
    @Autowired
    private CausalUnitOfWorkService causalUnitOfWorkService;

    @Override
    public Object handle(Command command, CommandHandler serviceCommandHandler) {
        return switch (command) {
            case CommitCausalCommand commitCausalCommand -> handleCommitCausal(commitCausalCommand);
            case PrepareCausalCommand prepareCausalCommand -> handlePrepareCausal(prepareCausalCommand);
            case AbortCausalCommand abortCausalCommand -> handleAbortCausal(abortCausalCommand);
            case GetConcurrentAggregateCommand getConcurrentAggregateCommand ->
                    handleGetConcurrentAggregate(getConcurrentAggregateCommand, serviceCommandHandler);
            case CausalCommand causalCommand -> serviceCommandHandler.handleDomainCommand(causalCommand.getPayload());
            default -> serviceCommandHandler.handleDomainCommand(command);
        };
    }

    private Object handleCommitCausal(CommitCausalCommand command) {
        logger.info("Committing causal for aggregate: " + command.getRootAggregateId());
        causalUnitOfWorkService.commitCausal(command.getAggregate());
        return null;
    }

    private Object handlePrepareCausal(PrepareCausalCommand command) {
        logger.info("Preparing causal for aggregate: " + command.getRootAggregateId());
        causalUnitOfWorkService.prepareCausal(command.getAggregate());
        return null;
    }

    private Object handleAbortCausal(AbortCausalCommand command) {
        logger.info("Aborting causal for aggregate: " + command.getRootAggregateId());
        causalUnitOfWorkService.abortCausal(command.getRootAggregateId());
        return null;
    }

    private Object handleGetConcurrentAggregate(GetConcurrentAggregateCommand command, CommandHandler serviceCommandHandler) {
        return causalUnitOfWorkService.getConcurrentAggregate(
                command.getRootAggregateId(),
                command.getVersion(),
                serviceCommandHandler.getAggregateTypeName());
    }
}
