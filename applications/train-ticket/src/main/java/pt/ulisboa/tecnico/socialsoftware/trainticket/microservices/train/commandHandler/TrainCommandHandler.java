package pt.ulisboa.tecnico.socialsoftware.trainticket.microservices.train.commandHandler;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import pt.ulisboa.tecnico.socialsoftware.ms.coordination.workflow.command.Command;
import pt.ulisboa.tecnico.socialsoftware.ms.coordination.workflow.command.CommandHandler;
import pt.ulisboa.tecnico.socialsoftware.trainticket.command.train.*;
import pt.ulisboa.tecnico.socialsoftware.trainticket.microservices.train.service.TrainService;

import java.util.logging.Logger;

@Component
public class TrainCommandHandler extends CommandHandler {
    private static final Logger logger = Logger.getLogger(TrainCommandHandler.class.getName());

    @Autowired
    private TrainService trainService;

    @Override
    protected String getAggregateTypeName() {
        return "Train";
    }

    @Override
    protected Object handleDomainCommand(Command command) {
        return switch (command) {
            case CreateTrainCommand cmd -> handleCreateTrain(cmd);
            case GetTrainByIdCommand cmd -> handleGetTrainById(cmd);
            case GetAllTrainsCommand cmd -> handleGetAllTrains(cmd);
            case UpdateTrainCommand cmd -> handleUpdateTrain(cmd);
            case DeleteTrainCommand cmd -> handleDeleteTrain(cmd);
            default -> {
                logger.warning("Unknown command type: " + command.getClass().getName());
                yield null;
            }
        };
    }

    private Object handleCreateTrain(CreateTrainCommand cmd) {
        logger.info("handleCreateTrain");
        try {
            return trainService.createTrain(cmd.getCreateRequest(), cmd.getUnitOfWork());
        } catch (Exception e) {
            logger.severe("Failed: " + e.getMessage());
            return e;
        }
    }

    private Object handleGetTrainById(GetTrainByIdCommand cmd) {
        logger.info("handleGetTrainById");
        try {
            return trainService.getTrainById(cmd.getRootAggregateId(), cmd.getUnitOfWork());
        } catch (Exception e) {
            logger.severe("Failed: " + e.getMessage());
            return e;
        }
    }

    private Object handleGetAllTrains(GetAllTrainsCommand cmd) {
        logger.info("handleGetAllTrains");
        try {
            return trainService.getAllTrains(cmd.getUnitOfWork());
        } catch (Exception e) {
            logger.severe("Failed: " + e.getMessage());
            return e;
        }
    }

    private Object handleUpdateTrain(UpdateTrainCommand cmd) {
        logger.info("handleUpdateTrain");
        try {
            return trainService.updateTrain(cmd.getTrainDto(), cmd.getUnitOfWork());
        } catch (Exception e) {
            logger.severe("Failed: " + e.getMessage());
            return e;
        }
    }

    private Object handleDeleteTrain(DeleteTrainCommand cmd) {
        logger.info("handleDeleteTrain");
        try {
            trainService.deleteTrain(cmd.getRootAggregateId(), cmd.getUnitOfWork());
            return null;
        } catch (Exception e) {
            logger.severe("Failed: " + e.getMessage());
            return e;
        }
    }
}
