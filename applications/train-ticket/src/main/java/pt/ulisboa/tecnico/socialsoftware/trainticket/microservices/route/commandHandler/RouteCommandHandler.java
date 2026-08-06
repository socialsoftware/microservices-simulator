package pt.ulisboa.tecnico.socialsoftware.trainticket.microservices.route.commandHandler;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import pt.ulisboa.tecnico.socialsoftware.ms.messaging.Command;
import pt.ulisboa.tecnico.socialsoftware.ms.messaging.CommandHandler;
import pt.ulisboa.tecnico.socialsoftware.trainticket.command.route.*;
import pt.ulisboa.tecnico.socialsoftware.trainticket.microservices.route.service.RouteService;

import java.util.logging.Logger;

@Component
public class RouteCommandHandler extends CommandHandler {
    private static final Logger logger = Logger.getLogger(RouteCommandHandler.class.getName());

    @Autowired
    private RouteService routeService;

    @Override
    public String getAggregateTypeName() {
        return "Route";
    }

    @Override
    public Object handleDomainCommand(Command command) {
        return switch (command) {
            case CreateRouteCommand cmd -> handleCreateRoute(cmd);
            case GetRouteByIdCommand cmd -> handleGetRouteById(cmd);
            case GetAllRoutesCommand cmd -> handleGetAllRoutes(cmd);
            case UpdateRouteCommand cmd -> handleUpdateRoute(cmd);
            case DeleteRouteCommand cmd -> handleDeleteRoute(cmd);
            default -> {
                logger.warning("Unknown command type: " + command.getClass().getName());
                yield null;
            }
        };
    }

    private Object handleCreateRoute(CreateRouteCommand cmd) {
        logger.info("handleCreateRoute");
        try {
            return routeService.createRoute(cmd.getCreateRequest(), cmd.getUnitOfWork());
        } catch (Exception e) {
            logger.severe("Failed: " + e.getMessage());
            return e;
        }
    }

    private Object handleGetRouteById(GetRouteByIdCommand cmd) {
        logger.info("handleGetRouteById");
        try {
            return routeService.getRouteById(cmd.getRootAggregateId(), cmd.getUnitOfWork());
        } catch (Exception e) {
            logger.severe("Failed: " + e.getMessage());
            return e;
        }
    }

    private Object handleGetAllRoutes(GetAllRoutesCommand cmd) {
        logger.info("handleGetAllRoutes");
        try {
            return routeService.getAllRoutes(cmd.getUnitOfWork());
        } catch (Exception e) {
            logger.severe("Failed: " + e.getMessage());
            return e;
        }
    }

    private Object handleUpdateRoute(UpdateRouteCommand cmd) {
        logger.info("handleUpdateRoute");
        try {
            return routeService.updateRoute(cmd.getRouteDto(), cmd.getUnitOfWork());
        } catch (Exception e) {
            logger.severe("Failed: " + e.getMessage());
            return e;
        }
    }

    private Object handleDeleteRoute(DeleteRouteCommand cmd) {
        logger.info("handleDeleteRoute");
        try {
            routeService.deleteRoute(cmd.getRootAggregateId(), cmd.getUnitOfWork());
            return null;
        } catch (Exception e) {
            logger.severe("Failed: " + e.getMessage());
            return e;
        }
    }
}
