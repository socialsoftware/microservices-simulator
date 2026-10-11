package pt.ulisboa.tecnico.socialsoftware.trainticket.microservices.traintype.messaging;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import pt.ulisboa.tecnico.socialsoftware.ms.messaging.Command;
import pt.ulisboa.tecnico.socialsoftware.ms.messaging.CommandHandler;
import pt.ulisboa.tecnico.socialsoftware.trainticket.commands.traintype.CreateTrainTypeCommand;
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
            case CreateTrainTypeCommand cmd -> handleCreateTrainType(cmd);
            default -> {
                logger.warning("Unknown command: " + command.getClass().getName());
                yield null;
            }
        };
    }

    private Object handleCreateTrainType(CreateTrainTypeCommand command) {
        return trainTypeService.createTrainType(command.getTrainTypeDto(), command.getUnitOfWork());
    }
}
