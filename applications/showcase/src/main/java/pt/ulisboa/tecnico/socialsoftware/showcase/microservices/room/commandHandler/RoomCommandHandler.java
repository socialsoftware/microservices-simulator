package pt.ulisboa.tecnico.socialsoftware.showcase.microservices.room.commandHandler;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import pt.ulisboa.tecnico.socialsoftware.ms.coordination.workflow.command.Command;
import pt.ulisboa.tecnico.socialsoftware.ms.coordination.workflow.command.CommandHandler;
import pt.ulisboa.tecnico.socialsoftware.showcase.command.room.*;
import pt.ulisboa.tecnico.socialsoftware.showcase.microservices.room.service.RoomService;

import java.util.logging.Logger;

@Component
public class RoomCommandHandler extends CommandHandler {
    private static final Logger logger = Logger.getLogger(RoomCommandHandler.class.getName());

    @Autowired
    private RoomService roomService;

    @Override
    protected String getAggregateTypeName() {
        return "Room";
    }

    @Override
    protected Object handleDomainCommand(Command command) {
        return switch (command) {
            case CreateRoomCommand cmd -> handleCreateRoom(cmd);
            case GetRoomByIdCommand cmd -> handleGetRoomById(cmd);
            case GetAllRoomsCommand cmd -> handleGetAllRooms(cmd);
            case UpdateRoomCommand cmd -> handleUpdateRoom(cmd);
            case DeleteRoomCommand cmd -> handleDeleteRoom(cmd);
            default -> {
                logger.warning("Unknown command type: " + command.getClass().getName());
                yield null;
            }
        };
    }

    private Object handleCreateRoom(CreateRoomCommand cmd) {
        logger.info("handleCreateRoom");
        try {
            return roomService.createRoom(cmd.getCreateRequest(), cmd.getUnitOfWork());
        } catch (RuntimeException e) {
            logger.severe("Failed: " + e.getMessage());
            throw e;
        }
    }

    private Object handleGetRoomById(GetRoomByIdCommand cmd) {
        logger.info("handleGetRoomById");
        try {
            return roomService.getRoomById(cmd.getRootAggregateId(), cmd.getUnitOfWork());
        } catch (RuntimeException e) {
            logger.severe("Failed: " + e.getMessage());
            throw e;
        }
    }

    private Object handleGetAllRooms(GetAllRoomsCommand cmd) {
        logger.info("handleGetAllRooms");
        try {
            return roomService.getAllRooms(cmd.getUnitOfWork());
        } catch (RuntimeException e) {
            logger.severe("Failed: " + e.getMessage());
            throw e;
        }
    }

    private Object handleUpdateRoom(UpdateRoomCommand cmd) {
        logger.info("handleUpdateRoom");
        try {
            return roomService.updateRoom(cmd.getRoomDto(), cmd.getUnitOfWork());
        } catch (RuntimeException e) {
            logger.severe("Failed: " + e.getMessage());
            throw e;
        }
    }

    private Object handleDeleteRoom(DeleteRoomCommand cmd) {
        logger.info("handleDeleteRoom");
        try {
            roomService.deleteRoom(cmd.getRootAggregateId(), cmd.getUnitOfWork());
            return null;
        } catch (RuntimeException e) {
            logger.severe("Failed: " + e.getMessage());
            throw e;
        }
    }
}
