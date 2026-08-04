package pt.ulisboa.tecnico.socialsoftware.trainticket.microservices.order.coordination.eventProcessing;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import pt.ulisboa.tecnico.socialsoftware.ms.coordination.unitOfWork.UnitOfWork;
import pt.ulisboa.tecnico.socialsoftware.ms.coordination.unitOfWork.UnitOfWorkService;
import pt.ulisboa.tecnico.socialsoftware.trainticket.microservices.order.service.OrderService;
import pt.ulisboa.tecnico.socialsoftware.trainticket.events.UserDeletedEvent;
import pt.ulisboa.tecnico.socialsoftware.trainticket.events.ContactsDeletedEvent;
import pt.ulisboa.tecnico.socialsoftware.trainticket.events.TripDeletedEvent;
import pt.ulisboa.tecnico.socialsoftware.trainticket.events.TrainDeletedEvent;
import pt.ulisboa.tecnico.socialsoftware.trainticket.events.StationDeletedEvent;

@Service
public class OrderEventProcessing {
    @Autowired
    private OrderService orderService;
    
    private final UnitOfWorkService<UnitOfWork> unitOfWorkService;

    public OrderEventProcessing(UnitOfWorkService unitOfWorkService) {
        this.unitOfWorkService = unitOfWorkService;
    }

    public void processUserDeletedEvent(Integer aggregateId, UserDeletedEvent userDeletedEvent) {
        // Reference constraint event processing - implement constraint logic
    }

    public void processContactsDeletedEvent(Integer aggregateId, ContactsDeletedEvent contactsDeletedEvent) {
        // Reference constraint event processing - implement constraint logic
    }

    public void processTripDeletedEvent(Integer aggregateId, TripDeletedEvent tripDeletedEvent) {
        // Reference constraint event processing - implement constraint logic
    }

    public void processTrainDeletedEvent(Integer aggregateId, TrainDeletedEvent trainDeletedEvent) {
        // Reference constraint event processing - implement constraint logic
    }

    public void processStationDeletedEvent(Integer aggregateId, StationDeletedEvent stationDeletedEvent) {
        // Reference constraint event processing - implement constraint logic
    }
}