package pt.ulisboa.tecnico.socialsoftware.trainticket.microservices.route.coordination.eventProcessing;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import pt.ulisboa.tecnico.socialsoftware.ms.coordination.unitOfWork.UnitOfWork;
import pt.ulisboa.tecnico.socialsoftware.ms.coordination.unitOfWork.UnitOfWorkService;
import pt.ulisboa.tecnico.socialsoftware.trainticket.microservices.route.service.RouteService;
import pt.ulisboa.tecnico.socialsoftware.trainticket.events.StationDeletedEvent;

@Service
public class RouteEventProcessing {
    @Autowired
    private RouteService routeService;
    
    private final UnitOfWorkService<UnitOfWork> unitOfWorkService;

    public RouteEventProcessing(UnitOfWorkService unitOfWorkService) {
        this.unitOfWorkService = unitOfWorkService;
    }

    public void processStationDeletedEvent(Integer aggregateId, StationDeletedEvent stationDeletedEvent) {
        // Reference constraint event processing - implement constraint logic
    }
}