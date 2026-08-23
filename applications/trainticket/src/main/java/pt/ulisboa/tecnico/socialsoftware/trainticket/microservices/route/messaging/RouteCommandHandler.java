package pt.ulisboa.tecnico.socialsoftware.trainticket.microservices.route.messaging;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import pt.ulisboa.tecnico.socialsoftware.ms.messaging.Command;
import pt.ulisboa.tecnico.socialsoftware.ms.messaging.CommandHandler;
import pt.ulisboa.tecnico.socialsoftware.trainticket.commands.route.GetRouteByIdCommand;
import pt.ulisboa.tecnico.socialsoftware.trainticket.commands.route.GetRoutesByStationCommand;
import pt.ulisboa.tecnico.socialsoftware.trainticket.commands.route.GetRoutesCommand;
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
            case GetRouteByIdCommand cmd -> handleGetRouteById(cmd);
            case GetRoutesCommand cmd -> handleGetRoutes(cmd);
            case GetRoutesByStationCommand cmd -> handleGetRoutesByStation(cmd);
            default -> {
                logger.warning("Unknown command: " + command.getClass().getName());
                yield null;
            }
        };
    }

    private Object handleGetRouteById(GetRouteByIdCommand command) {
        return routeService.getRouteById(command.getRouteAggregateId(), command.getUnitOfWork());
    }

    private Object handleGetRoutes(GetRoutesCommand command) {
        return routeService.getRoutes(command.getUnitOfWork());
    }

    private Object handleGetRoutesByStation(GetRoutesByStationCommand command) {
        return routeService.getRoutesByStation(command.getStationAggregateId(), command.getUnitOfWork());
    }
}
