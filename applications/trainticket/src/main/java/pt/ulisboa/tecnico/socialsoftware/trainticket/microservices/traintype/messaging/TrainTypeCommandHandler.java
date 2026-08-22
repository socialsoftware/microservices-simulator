package pt.ulisboa.tecnico.socialsoftware.trainticket.microservices.traintype.messaging;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import pt.ulisboa.tecnico.socialsoftware.ms.messaging.Command;
import pt.ulisboa.tecnico.socialsoftware.ms.messaging.CommandHandler;
import pt.ulisboa.tecnico.socialsoftware.trainticket.commands.traintype.GetTrainTypeByIdCommand;
import pt.ulisboa.tecnico.socialsoftware.trainticket.commands.traintype.GetTrainTypesCommand;
import pt.ulisboa.tecnico.socialsoftware.trainticket.microservices.traintype.service.TrainTypeService;

import java.util.logging.Logger;

@Component
public class TrainTypeCommandHandler extends CommandHandler {
    private static final Logger logger = Logger.getLogger(TrainTypeCommandHandler.class.getName());

    @Autowired
    private TrainTypeService trainTypeService;

    @Override
    public String getAggregateTypeName() {
        return "TrainType";
    }

    @Override
    public Object handleDomainCommand(Command command) {
        return switch (command) {
            case GetTrainTypeByIdCommand cmd -> handleGetTrainTypeById(cmd);
            case GetTrainTypesCommand cmd -> handleGetTrainTypes(cmd);
            default -> {
                logger.warning("Unknown command: " + command.getClass().getName());
                yield null;
            }
        };
    }

    private Object handleGetTrainTypeById(GetTrainTypeByIdCommand command) {
        return trainTypeService.getTrainTypeById(command.getTrainTypeAggregateId(), command.getUnitOfWork());
    }

    private Object handleGetTrainTypes(GetTrainTypesCommand command) {
        return trainTypeService.getTrainTypes(command.getUnitOfWork());
    }
}
