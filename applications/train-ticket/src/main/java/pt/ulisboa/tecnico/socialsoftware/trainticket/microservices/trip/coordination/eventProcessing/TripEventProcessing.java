package pt.ulisboa.tecnico.socialsoftware.trainticket.microservices.trip.coordination.eventProcessing;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import pt.ulisboa.tecnico.socialsoftware.ms.transaction.unitOfWork.UnitOfWork;
import pt.ulisboa.tecnico.socialsoftware.ms.transaction.unitOfWork.UnitOfWorkService;
import pt.ulisboa.tecnico.socialsoftware.trainticket.microservices.trip.service.TripService;
import pt.ulisboa.tecnico.socialsoftware.trainticket.events.TrainDeletedEvent;
import pt.ulisboa.tecnico.socialsoftware.trainticket.events.RouteDeletedEvent;
import pt.ulisboa.tecnico.socialsoftware.trainticket.events.StationDeletedEvent;

@Service
public class TripEventProcessing {
    @Autowired
    private TripService tripService;
    
    private final UnitOfWorkService<UnitOfWork> unitOfWorkService;

    public TripEventProcessing(UnitOfWorkService unitOfWorkService) {
        this.unitOfWorkService = unitOfWorkService;
    }

    public void processTrainDeletedEvent(Integer aggregateId, TrainDeletedEvent trainDeletedEvent) {
        // Reference constraint event processing - implement constraint logic
    }

    public void processRouteDeletedEvent(Integer aggregateId, RouteDeletedEvent routeDeletedEvent) {
        // Reference constraint event processing - implement constraint logic
    }

    public void processStationDeletedEvent(Integer aggregateId, StationDeletedEvent stationDeletedEvent) {
        // Reference constraint event processing - implement constraint logic
    }
}