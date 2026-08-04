package pt.ulisboa.tecnico.socialsoftware.trainticket.microservices.payment.coordination.eventProcessing;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import pt.ulisboa.tecnico.socialsoftware.ms.coordination.unitOfWork.UnitOfWork;
import pt.ulisboa.tecnico.socialsoftware.ms.coordination.unitOfWork.UnitOfWorkService;
import pt.ulisboa.tecnico.socialsoftware.trainticket.microservices.payment.service.PaymentService;
import pt.ulisboa.tecnico.socialsoftware.trainticket.events.OrderDeletedEvent;
import pt.ulisboa.tecnico.socialsoftware.trainticket.events.UserDeletedEvent;

@Service
public class PaymentEventProcessing {
    @Autowired
    private PaymentService paymentService;
    
    private final UnitOfWorkService<UnitOfWork> unitOfWorkService;

    public PaymentEventProcessing(UnitOfWorkService unitOfWorkService) {
        this.unitOfWorkService = unitOfWorkService;
    }

    public void processOrderDeletedEvent(Integer aggregateId, OrderDeletedEvent orderDeletedEvent) {
        // Reference constraint event processing - implement constraint logic
    }

    public void processUserDeletedEvent(Integer aggregateId, UserDeletedEvent userDeletedEvent) {
        // Reference constraint event processing - implement constraint logic
    }
}