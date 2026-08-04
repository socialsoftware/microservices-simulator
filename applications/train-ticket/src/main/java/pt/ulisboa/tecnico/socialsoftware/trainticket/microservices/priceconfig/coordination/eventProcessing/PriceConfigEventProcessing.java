package pt.ulisboa.tecnico.socialsoftware.trainticket.microservices.priceconfig.coordination.eventProcessing;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import pt.ulisboa.tecnico.socialsoftware.ms.coordination.unitOfWork.UnitOfWork;
import pt.ulisboa.tecnico.socialsoftware.ms.coordination.unitOfWork.UnitOfWorkService;
import pt.ulisboa.tecnico.socialsoftware.trainticket.microservices.priceconfig.service.PriceConfigService;
import pt.ulisboa.tecnico.socialsoftware.trainticket.events.TrainDeletedEvent;
import pt.ulisboa.tecnico.socialsoftware.trainticket.events.RouteDeletedEvent;

@Service
public class PriceConfigEventProcessing {
    @Autowired
    private PriceConfigService priceconfigService;
    
    private final UnitOfWorkService<UnitOfWork> unitOfWorkService;

    public PriceConfigEventProcessing(UnitOfWorkService unitOfWorkService) {
        this.unitOfWorkService = unitOfWorkService;
    }

    public void processTrainDeletedEvent(Integer aggregateId, TrainDeletedEvent trainDeletedEvent) {
        // Reference constraint event processing - implement constraint logic
    }

    public void processRouteDeletedEvent(Integer aggregateId, RouteDeletedEvent routeDeletedEvent) {
        // Reference constraint event processing - implement constraint logic
    }
}